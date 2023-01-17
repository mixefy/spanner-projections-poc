package jp.mixefy.spannerprojections;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountProjection getAccountById(String accountId) {
        return accountRepository.findProjectionById(accountId);
    }

}
