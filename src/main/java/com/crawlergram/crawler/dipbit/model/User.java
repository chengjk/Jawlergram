package com.crawlergram.crawler.dipbit.model;

import com.crawlergram.crawler.apimethods.ContactMethods;
import lombok.Getter;
import lombok.Setter;
import org.telegram.api.engine.TelegramApi;
import org.telegram.api.input.user.TLInputUser;
import org.telegram.api.user.TLUser;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * created by jacky. 2019/4/3 10:09 AM
 */
@Getter
@Setter
public class User {
    private Integer id;
    private long accessHash;
    private String username;
    private String firstName;
    private String lastName;
    private String phone;
    private String langCode;


    public TLInputUser toTlInputUser() {
        TLInputUser inputUser = new TLInputUser();
        inputUser.setUserId(id);
        inputUser.setAccessHash(accessHash);
        return inputUser;
    }


    public User resolveUsername(TelegramApi api) throws IOException, TimeoutException {
        TLUser tlUser = ContactMethods.resolveUsername(api, username);
        this.id = tlUser.getId();
        this.accessHash = tlUser.getAccessHash();
        this.firstName = tlUser.getFirstName();
        this.lastName = tlUser.getLastName();
        this.phone = tlUser.getPhone();
        this.langCode = tlUser.getLangCode();
        return this;
    }


}
