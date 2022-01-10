package guests.domain;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;

@Entity(name = "scim_failures")
@NoArgsConstructor
@Getter
@Setter
public class SCIMFailure implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String message;

    @Column(name = "http_method")
    @NotNull
    private String httpMethod;

    @Column(name = "uri")
    @NotNull
    private String uri;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "application_id")
    private Application application;

    @Column(name = "created_at")
    private Instant createdAt;

    public SCIMFailure(String message, String httpMethod, String uri, Application application) {
        this.message = message;
        this.httpMethod = httpMethod;
        this.uri = uri;
        this.application = application;
        this.createdAt = Instant.now();
    }
}
