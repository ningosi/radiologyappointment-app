package org.openmrs.module.radiologyapp.page.controller;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.dcm4che2.imageio.plugins.dcm.DicomImageReadParam;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.appui.UiSessionContext;
import org.openmrs.module.hospitalcore.HospitalCoreService;
import org.openmrs.module.hospitalcore.RadiologyService;
import org.openmrs.module.hospitalcore.model.RadiologyTest;
import org.openmrs.module.referenceapplication.ReferenceApplicationWebConstants;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.page.PageModel;
import org.openmrs.ui.framework.page.PageRequest;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.web.bind.annotation.RequestParam;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;


/**
 * @author Stanslaus Odhiambo
 *         Created on 7/20/2016.
 */
public class PatientReportPageController {
    public static final String ROOT = "complex_obs";

    public String get(
            UiSessionContext sessionContext, @RequestParam(value = "testId") Integer testId,
            PageModel model, @RequestParam(value = "encounterId") Integer encounterId,
            UiUtils ui,
            PageRequest pageRequest) {
        pageRequest.getSession().setAttribute(ReferenceApplicationWebConstants.SESSION_ATTRIBUTE_REDIRECT_URL, ui.thisUrl());
        sessionContext.requireAuthentication();
        Boolean isPriviledged = Context.hasPrivilege("Access Laboratory");
        if (!isPriviledged) {
            return "redirect: index.htm";
        }
        RadiologyService rs = Context.getService(RadiologyService.class);
        RadiologyTest radiologyTest = rs.getRadiologyTestById(testId);
        Patient patient = radiologyTest.getPatient();
        HospitalCoreService hcs = Context.getService(HospitalCoreService.class);

        model.addAttribute("patient", patient);
        model.addAttribute("radiologyTest", radiologyTest.getConcept().getName().getName());
        model.addAttribute("patientIdentifier", patient.getPatientIdentifier());
        model.addAttribute("age", patient.getAge());
        model.addAttribute("gender", patient.getGender());
        model.addAttribute("name", patient.getNames());
        model.addAttribute("category", patient.getAttribute(14));
        model.addAttribute("previousVisit", hcs.getLastVisitTime(patient));
        if (patient.getAttribute(43) == null) {
            model.addAttribute("fileNumber", "");
        } else if (StringUtils.isNotBlank(patient.getAttribute(43).getValue())) {
            model.addAttribute("fileNumber", "(File: " + patient.getAttribute(43) + ")");
        } else {
            model.addAttribute("fileNumber", "");
        }

        Encounter encounter = Context.getEncounterService().getEncounter(encounterId);
        Set<Obs> allObs = encounter.getAllObs();

        for (Obs obs : allObs) {
            model.addAttribute("_" + obs.getConcept().getConceptId(),
                    obs.getValueText() == null ? obs.getValueCoded().getName().getName() : obs.getValueText());
            if (obs.getConcept().getConceptId() == 100126232) {
//               load the image file
                File imgDir = new File(OpenmrsUtil.getApplicationDataDirectory(), ROOT);
                File imgFile = new File(imgDir, obs.getValueText());
                Image img = null;
                BufferedImage image = null;
                try {
                    image=getPixelDataAsBufferedImage(IOUtils.toByteArray(new FileInputStream(imgFile)));

                } catch (IOException e) {
                    System.out.println("\nError: couldn't read dicom image!"+ e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                model.addAttribute("imgFile", img);
                model.addAttribute("imgFileRaw", imgFile);
            }
        }


        return null;
    }

    public static BufferedImage getPixelDataAsBufferedImage(byte[] dicomData)
            throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(dicomData);
        BufferedImage buff = null;
        Iterator<ImageReader> iter = ImageIO.getImageReadersByFormatName("DICOM");
        ImageReader reader = (ImageReader) iter.next();
        DicomImageReadParam param = (DicomImageReadParam) reader.getDefaultReadParam();
        ImageInputStream iis = ImageIO.createImageInputStream(bais);
        reader.setInput(iis, false);
        buff = reader.read(0, param);
        iis.close();
        if (buff == null) {
            throw new IOException("Could not read Dicom file. Maybe pixel data is invalid.");
        }
        return buff;
    }

}
