package jp.mixefy.spannerprojections;

import com.google.cloud.spring.data.spanner.repository.SpannerRepository;
import com.google.cloud.spring.data.spanner.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends SpannerRepository<Account, String> {
    AccountProjection findProjectionById(String accountId);

    @Query("SELECT id, name FROM accounts WHERE id = @accountId")
    AccountProjection findProjectionUsingQuery(String accountId);
}
