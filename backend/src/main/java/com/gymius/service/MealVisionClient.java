package com.gymius.service;

import com.gymius.dto.MealAnalysisDto;
import org.springframework.web.multipart.MultipartFile;

public interface MealVisionClient {

    MealAnalysisDto analyze(MultipartFile image);
}
