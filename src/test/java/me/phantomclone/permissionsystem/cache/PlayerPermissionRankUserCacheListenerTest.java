package me.phantomclone.permissionsystem.cache;

import me.phantomclone.permissionsystem.entity.PermissionRankUser;
import me.phantomclone.permissionsystem.event.PermissionRankUserUpdateEvent;
import me.phantomclone.permissionsystem.service.UserPermissionRankService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerPermissionRankUserCacheListenerTest {

    @Mock
    private UserPermissionRankService userPermissionRankService;

    @InjectMocks
    private PlayerPermissionRankUserCacheListener listener;

    private UUID uuid;
    private PermissionRankUser permissionRankUser;

    @BeforeEach
    public void setUp() {
        uuid = UUID.randomUUID();
        permissionRankUser = mock(PermissionRankUser.class);

        listener = new PlayerPermissionRankUserCacheListener(userPermissionRankService);
    }

    @Test
    public void testOnPermissionRankUserUpdateEvent() {
        PermissionRankUserUpdateEvent event = mock(PermissionRankUserUpdateEvent.class);

        when(event.getPermissionRankUser()).thenReturn(permissionRankUser);
        when(permissionRankUser.uuid()).thenReturn(uuid);

        listener.onPermissionRankUserUpdateEvent(event);

        assertEquals(permissionRankUser, listener.getPermissionRankUser(uuid).join());
    }

    @Test
    public void testGetPermissionRankUserFromCache() {
        when(permissionRankUser.uuid()).thenReturn(uuid);

        listener.onPermissionRankUserUpdateEvent(new PermissionRankUserUpdateEvent(permissionRankUser));

        CompletableFuture<PermissionRankUser> result = listener.getPermissionRankUser(uuid);

        assertTrue(result.isDone());
        assertEquals(permissionRankUser, result.join());
        verify(userPermissionRankService, never()).getPermissionRankUser(any());
    }

    @Test
    public void testGetPermissionRankUserFromService() {
        CompletableFuture<PermissionRankUser> permissionRankUserCompletableFuture = new CompletableFuture<>();
        when(userPermissionRankService.getPermissionRankUser(uuid))
                .thenReturn(permissionRankUserCompletableFuture);

        CompletableFuture<PermissionRankUser> result = listener.getPermissionRankUser(uuid);

        assertFalse(result.isDone());

        permissionRankUserCompletableFuture.complete(permissionRankUser);

        assertEquals(permissionRankUser, result.join());

        verify(userPermissionRankService, times(1)).getPermissionRankUser(uuid);
        assertEquals(permissionRankUser, listener.getPermissionRankUser(uuid).join());
    }

}
