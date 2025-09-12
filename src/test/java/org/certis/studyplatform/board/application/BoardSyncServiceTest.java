package org.certis.studyplatform.board.application;

import org.certis.studyplatform.board.application.sync.BoardSyncService;
import org.certis.studyplatform.board.domain.service.BoardDomainService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BoardSyncServiceTest {

    @Mock
    private BoardDomainService boardDomainService;

    @InjectMocks
    private BoardSyncService boardSyncService;

    @Test
    @DisplayName("syncStatsFromRedisToDatabase는 DomainService로 위임한다")
    void syncStatsFromRedisToDatabase_delegatesToDomain() {
        boardSyncService.syncStatsFromRedisToDatabase();
        verify(boardDomainService, times(1)).syncAllBoardStats();
    }
}


