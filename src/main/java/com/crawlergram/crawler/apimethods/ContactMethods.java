package com.crawlergram.crawler.apimethods;

import org.telegram.api.contacts.TLResolvedPeer;
import org.telegram.api.engine.TelegramApi;
import org.telegram.api.functions.contacts.TLRequestContactsResolveUsername;
import org.telegram.api.user.TLUser;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * created by jacky. 2019/4/2 6:14 PM
 */
public class ContactMethods {
    public static TLUser resolveUsername(TelegramApi api, String username) throws IOException, TimeoutException {
        TLRequestContactsResolveUsername req=new TLRequestContactsResolveUsername();
        req.setUsername(username);
        TLResolvedPeer resp = api.doRpcCall(req);
        TLUser user = (TLUser) resp.getUsers().get(0);
        return user;
    }

}
