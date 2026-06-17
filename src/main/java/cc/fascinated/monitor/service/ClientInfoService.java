package cc.fascinated.monitor.service;

import cc.fascinated.monitor.model.domain.ClientInfo;
import cc.fascinated.monitor.util.ClientRequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class ClientInfoService {
    private final EncryptionService encryptionService;

    public ClientInfoService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public ClientInfo capture(HttpServletRequest request) {
        String ip = ClientRequestUtils.clientIp(request);
        String userAgent = ClientRequestUtils.userAgent(request);
        String ipEncrypted = ip != null && !ip.isBlank() ? this.encryptionService.encrypt(ip) : null;
        return new ClientInfo(ipEncrypted, userAgent);
    }
}
