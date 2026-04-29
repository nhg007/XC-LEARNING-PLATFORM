package com.xc.study.module.admin.service;

import com.xc.study.module.admin.vo.AdminContentImportResultVO;
import com.xc.study.module.admin.vo.AdminContentImportTemplateVO;
import com.xc.study.security.CurrentUser;
import org.springframework.web.multipart.MultipartFile;

public interface AdminContentImportService {

    AdminContentImportTemplateVO template(String importType, CurrentUser admin);

    AdminContentImportResultVO importCsv(String importType, MultipartFile file, CurrentUser admin, String ipAddress);
}
