package plus.maa.backend.config.external;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class Commit {
    private String abbreviatedId;
    private ZonedDateTime dateTime;
}
