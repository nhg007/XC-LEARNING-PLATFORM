package com.xc.study.module.admin.service;

import com.xc.study.module.admin.vo.AdminContentImportResultVO;
import com.xc.study.module.admin.vo.AdminContentImportTemplateVO;
import com.xc.study.security.CurrentUser;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface AdminContentImportService {

    AdminContentImportTemplateVO template(String importType, Long contextId, List<Long> contextIds, String contextName, CurrentUser admin);

    AdminContentImportResultVO importCsv(String importType, MultipartFile file, Long contextId, List<Long> contextIds, CurrentUser admin, String ipAddress);
}
