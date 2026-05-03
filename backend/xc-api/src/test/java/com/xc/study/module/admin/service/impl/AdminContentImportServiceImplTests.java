package com.xc.study.module.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xc.study.module.admin.dto.AdminUpsertVocabItemDTO;
import com.xc.study.module.admin.mapper.AdminOperationLogMapper;
import com.xc.study.module.admin.service.AdminDialogueManagementService;
import com.xc.study.module.admin.service.AdminExerciseManagementService;
import com.xc.study.module.admin.service.AdminVocabManagementService;
import com.xc.study.module.admin.vo.AdminContentImportResultVO;
import com.xc.study.security.CurrentUser;
import com.xc.study.security.UserType;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class AdminContentImportServiceImplTests {

    @Test
    void importVocabItemsAllowsMissingAudioAssetColumn() {
        AdminVocabManagementService vocabService = mock(AdminVocabManagementService.class);
        AdminContentImportServiceImpl service = new AdminContentImportServiceImpl(
                vocabService,
                mock(AdminExerciseManagementService.class),
                mock(AdminDialogueManagementService.class),
                mock(AdminOperationLogMapper.class),
                new ObjectMapper()
        );
        CurrentUser admin = admin();
        MockMultipartFile file = csvFile("""
                记录ID（更新时填）,汉字/词语,拼音,英文释义,俄文释义,例句,排序,状态
                ,中国,zhong guo,China,Китай,我来自中国。,0,active
                """);

        AdminContentImportResultVO result = service.importCsv("vocab-items", file, 12L, admin, "127.0.0.1");

        assertEquals(1, result.requestedCount());
        assertEquals(1, result.successCount());
        assertEquals(0, result.errors().size());
        ArgumentCaptor<AdminUpsertVocabItemDTO> captor = ArgumentCaptor.forClass(AdminUpsertVocabItemDTO.class);
        verify(vocabService).createItem(captor.capture(), eq(admin), eq("127.0.0.1"));
        assertEquals(12L, captor.getValue().vocabListId());
        assertNull(captor.getValue().audioAssetId());
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
                "file",
                "词汇条目导入模板.csv",
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private CurrentUser admin() {
        return new CurrentUser(
                1L,
                "admin",
                UserType.ADMIN,
                Set.of("admin"),
                Set.of("admin:content:update")
        );
    }
}
