
package com.crawlergram.crawler.apimethods;

import lombok.extern.slf4j.Slf4j;
import org.telegram.api.engine.TelegramApi;
import org.telegram.api.functions.channels.TLRequestChannelsDeleteMessages;
import org.telegram.api.functions.messages.TLRequestMessagesGetHistory;
import org.telegram.api.input.chat.TLInputChannel;
import org.telegram.api.input.peer.TLInputPeerChannel;
import org.telegram.api.message.TLAbsMessage;
import org.telegram.api.message.TLMessageService;
import org.telegram.api.message.action.TLMessageActionChatAddUser;
import org.telegram.api.messages.TLAbsMessages;
import org.telegram.api.messages.TLAffectedMessages;
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
    public static Set<TLAbsMessage> getHistory(TelegramApi api, TLInputChannel channel,int limit) {
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
            log.error("", e);
        }
        return null;
    }


    public static void deleteMessage(TelegramApi api, TLInputChannel channel, Set<Integer> msgIds) {
        TLRequestChannelsDeleteMessages req = new TLRequestChannelsDeleteMessages();
        req.setChannel(channel);
        TLIntVector idVector = new TLIntVector();
        idVector.addAll(msgIds);
        req.setId(idVector);
        try {
            TLAffectedMessages affectedMessages = api.doRpcCall(req);
            System.out.println("ASdf");
        } catch (IOException e) {
            log.error("", e);
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
