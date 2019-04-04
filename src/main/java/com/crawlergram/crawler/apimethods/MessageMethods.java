
package com.crawlergram.crawler.apimethods;

import lombok.extern.slf4j.Slf4j;
import org.telegram.api.chat.channel.TLChannel;
import org.telegram.api.contacts.TLResolvedPeer;
import org.telegram.api.engine.TelegramApi;
import org.telegram.api.functions.channels.TLRequestChannelsDeleteMessages;
import org.telegram.api.functions.channels.TLRequestChannelsJoinChannel;
import org.telegram.api.functions.contacts.TLRequestContactsResolveUsername;
import org.telegram.api.functions.messages.TLRequestMessagesGetHistory;
import org.telegram.api.functions.messages.TLRequestMessagesImportChatInvite;
import org.telegram.api.input.chat.TLInputChannel;
import org.telegram.api.input.peer.TLInputPeerChannel;
import org.telegram.api.message.TLAbsMessage;
import org.telegram.api.message.TLMessageService;
import org.telegram.api.message.action.TLMessageActionChatAddUser;
import org.telegram.api.messages.TLAbsMessages;
import org.telegram.api.messages.TLAffectedMessages;
import org.telegram.api.updates.TLAbsUpdates;
import org.telegram.tl.TLIntVector;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * created by jacky. 2019/3/21 4:43 PM
 */
@Slf4j
public class MessageMethods {
    public static Set<TLAbsMessage> getHistory(TelegramApi api, TLInputChannel channel, int limit) {
        TLRequestMessagesGetHistory req = new TLRequestMessagesGetHistory();
        req.setLimit(limit);
        req.setMaxId(-1);
        TLInputPeerChannel peer = new TLInputPeerChannel();
        peer.setChannelId(channel.getChannelId());
        peer.setAccessHash(channel.getAccessHash());
        req.setPeer(peer);
        try {
            TLAbsMessages messages = api.doRpcCall(req);

            Set<TLAbsMessage> addUserMessage = messages.getMessages().stream()
                    .filter(f -> f instanceof TLMessageService)
                    .filter(f -> ((TLMessageService) f).getAction() instanceof TLMessageActionChatAddUser)
//                    .filter(f -> ((TLMessageService) f).getFromId() == 478702429)
                    .collect(Collectors.toSet());
            //5848
            return addUserMessage;
        } catch (IOException | TimeoutException e) {
            log.error("getHistory ", e);
        }
        return null;
    }


    public static void deleteMessage(TelegramApi api, TLInputChannel channel, Set<Integer> msgIds) {
        if (msgIds == null || msgIds.isEmpty()) {
            return;
        }
        TLRequestChannelsDeleteMessages req = new TLRequestChannelsDeleteMessages();
        req.setChannel(channel);
        TLIntVector idVector = new TLIntVector();
        idVector.addAll(msgIds);
        req.setId(idVector);
        try {
            TLAffectedMessages affectedMessages = api.doRpcCall(req);
            log.info("clear {} notification message finish.", msgIds.size());
        } catch (IOException e) {
            log.error("timeout ", e);
        } catch (Exception e) {
            log.error("err", e);
        }
    }


    /**
     *
     * @param api
     * @param link http://telegram.me/xxxxx,https://telegram.me/joinchat/xxxxx
     */
    public static void joinToChat(TelegramApi api, String link) {
        if (link.contains("telegram.me/joinchat")) {
            String hash = link.split("/")[(link.split("/").length) - 1];
            TLRequestMessagesImportChatInvite in = new TLRequestMessagesImportChatInvite();
            in.setHash(hash);
            try {
                TLAbsUpdates bb = api.doRpcCall(in);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (link.contains("telegram.me/")) {
            String username = link.split("/")[(link.split("/").length) - 1];
            try {
                TLRequestContactsResolveUsername ru = new TLRequestContactsResolveUsername();
                ru.setUsername(username);
                TLResolvedPeer peer = api.doRpcCall(ru);
                TLRequestChannelsJoinChannel join = new TLRequestChannelsJoinChannel();
                TLInputChannel ch = new TLInputChannel();
                ch.setChannelId(peer.getChats().get(0).getId());
                ch.setAccessHash(((TLChannel) peer.getChats().get(0)).getAccessHash());
                join.setChannel(ch);
                api.doRpcCall(join);
            } catch (Exception e) {
                log.error("join to channel error.", e);
            }
        }
    }
}
