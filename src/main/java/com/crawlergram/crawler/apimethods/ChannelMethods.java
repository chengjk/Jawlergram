/*
 * Title: ChannelMethods.java
 * Project: Jawlergram
 * Creator: Georgii Mikriukov
 * 2019
 */

package com.crawlergram.crawler.apimethods;


import lombok.extern.slf4j.Slf4j;
import org.telegram.api.channel.TLChannelParticipants;
import org.telegram.api.channel.participants.filters.TLChannelParticipantsFilterRecent;
import org.telegram.api.engine.TelegramApi;
import org.telegram.api.functions.channels.TLRequestChannelsGetParticipants;
import org.telegram.api.functions.channels.TLRequestChannelsInviteToChannel;
import org.telegram.api.input.chat.TLInputChannel;
import org.telegram.api.input.user.TLAbsInputUser;
import org.telegram.api.updates.TLAbsUpdates;
import org.telegram.api.user.TLAbsUser;
import org.telegram.tl.TLVector;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * created by jacky. 2019/3/20 9:57 AM
 */
@Slf4j
public class ChannelMethods {


    public static TLVector<TLAbsUser> getUsers(TelegramApi api, TLInputChannel channel) {
        try {

            TLRequestChannelsGetParticipants req = new TLRequestChannelsGetParticipants();
            req.setChannel(channel);
            req.setOffset(0);
            req.setLimit(500);
            req.setFilter(new TLChannelParticipantsFilterRecent());
            TLChannelParticipants resp = api.doRpcCall(req);
            TLVector<TLAbsUser> users = resp.getUsers();
            int count = resp.getCount();
            while (users.size()<count){
                TLChannelParticipants respPage = api.doRpcCall(req);
                users.addAll(respPage.getUsers());
            }
            return users;
        } catch (IOException | TimeoutException e) {
            log.error("",e);
        }
        return null;
    }


    public static void inviteUser(TelegramApi api, TLInputChannel channel, TLAbsInputUser user) {

        TLVector<TLAbsInputUser> users = new TLVector<>();
        users.add(user);
        inviteUsers(api, channel, users);

    }

    public static void inviteUsers(TelegramApi api, TLInputChannel channel, TLVector<TLAbsInputUser> users) {

        try {
            TLRequestChannelsInviteToChannel req = new TLRequestChannelsInviteToChannel();
            req.setChannel(channel);
            req.setUsers(users);
            TLAbsUpdates tlAbsUpdates = api.doRpcCall(req);
            System.out.println("sdfa");
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }


}
