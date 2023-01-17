package jp.mixefy.spannerprojections;

import com.google.cloud.spring.data.spanner.core.mapping.Column;
import com.google.cloud.spring.data.spanner.core.mapping.PrimaryKey;
import com.google.cloud.spring.data.spanner.core.mapping.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(name = "accounts")
public class Account {

    @PrimaryKey
    @Column(name = "id")
    private String id;

    @Column(name="name")
    private String name;

    @Column(name="creation_timestamp", spannerCommitTimestamp = true)
    private LocalDateTime timestamp;
}
