package cc.fascinated.monitor.model.persistance;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ServerMemberId implements Serializable {
    private Long serverId;
    private Long userId;
}
