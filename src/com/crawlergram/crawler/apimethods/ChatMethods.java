/*
 * Title: ChatMethods.java
 * Project: Jawlergram
 * Creator: Georgii Mikriukov
 * 2019
 */

package com.crawlergram.crawler.apimethods;

import org.telegram.api.chat.TLAbsChat;
import org.telegram.api.engine.Logger;
import org.telegram.api.engine.RpcCallback;
import org.telegram.api.engine.TelegramApi;
import org.telegram.api.functions.channels.TLRequestChannelsDeleteMessages;
import org.telegram.api.functions.messages.TLRequestMessagesAddChatUser;
import org.telegram.api.functions.messages.TLRequestMessagesDeleteHistory;
import org.telegram.api.functions.messages.TLRequestMessagesGetFullChat;
import org.telegram.api.input.peer.TLInputPeerChannel;
import org.telegram.api.input.user.TLInputUser;
import org.telegram.api.messages.TLMessagesChatFull;
import org.telegram.api.updates.TLAbsUpdates;
import org.telegram.api.user.TLAbsUser;
import org.telegram.tl.TLVector;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * created by jacky. 2019/3/19 5:51 PM
 */
public class ChatMethods {



    public static void addUser(TelegramApi api, int chatId, List<TLInputUser> users) {
        users.forEach(user->{
            TLRequestMessagesAddChatUser req = new TLRequestMessagesAddChatUser();
            req.setChatId(chatId);
            req.setUserId(user);
            api.doRpcCall(req, new RpcCallback<TLAbsUpdates>() {
                @Override
                public void onResult(TLAbsUpdates result) {
                    System.out.println("add finish");
                }
                @Override
                public void onError(int errorCode, String message) {
                    Logger.d(errorCode + "", message);
                }
            });
        });
    }


    public static TLVector<TLAbsChat> deleteMessage(TelegramApi api) {
//        TLRequestChannelsDeleteMessages req=new TLRequestChannelsDeleteMessages();
//        req.setChannel();
//        req.setId();
        return null;
    }


    public static TLVector<TLAbsUser> getUsers(TelegramApi api, int chatId){
        try {
            TLRequestMessagesGetFullChat req = new TLRequestMessagesGetFullChat();
            req.setChatId(chatId);
            TLMessagesChatFull chatFull = api.doRpcCall(req);
            return chatFull.getUsers();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (TimeoutException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }





}
