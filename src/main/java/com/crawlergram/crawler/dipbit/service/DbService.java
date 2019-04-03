package com.crawlergram.crawler.dipbit.service;

import com.alibaba.fastjson.JSON;
import com.crawlergram.crawler.apimethods.ChannelMethods;
import com.crawlergram.crawler.apimethods.MessageMethods;
import com.crawlergram.crawler.dipbit.model.User;
import com.crawlergram.crawler.output.ConsoleOutputMethods;
import com.crawlergram.crawler.output.FileMethods;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.telegram.api.channel.TLChannelParticipants;
import org.telegram.api.chat.TLAbsChat;
import org.telegram.api.chat.channel.TLChannel;
import org.telegram.api.dialog.TLDialog;
import org.telegram.api.engine.RpcException;
import org.telegram.api.engine.TelegramApi;
import org.telegram.api.input.chat.TLInputChannel;
import org.telegram.api.input.user.TLAbsInputUser;
import org.telegram.api.message.TLAbsMessage;
import org.telegram.api.message.TLMessageService;
import org.telegram.api.user.TLAbsUser;
import org.telegram.api.user.TLUser;
import org.telegram.tl.TLVector;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * created by jacky. 2019/4/3 11:20 AM
 */
@Slf4j
@Getter
@Setter
@NoArgsConstructor
public class DbService {
    private TelegramApi api;
    private static String blackListFilePath = "./contacts/black/blacklist.contact";
    private Map<Integer, String> blackListMap = new HashMap<>();

    private User me;
    private Map<Integer, TLAbsChat> chatsHashMap = new HashMap<>();
    private Map<Integer, TLAbsUser> usersHashMap = new HashMap<>();
    private TLVector<TLDialog> dialogs = new TLVector<>();
    private Map<Integer, TLAbsMessage> messagesHashMap = new HashMap<>();


    private int invitePageSize = 5;
    private int inviteLimitPerAccount = 20;
    private long inviteIntervalMs = 10;
    private boolean inviteSelfContact = false;
    private boolean notifications = false;


    public Map<Integer, String> loadBlacklist() throws IOException {
        File file = new File(blackListFilePath);
        if (file.exists()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(blackListFilePath)));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty()) {
                    String[] split = line.split(",");
                    blackListMap.put(Integer.valueOf(split[0]), split[1]);
                }
            }
            log.info("load black list finish. size:{}", blackListMap.size());
            br.close();
        }
        return blackListMap;
    }

    public void inviteContactToChannel(int channelId, String filePath) {
        try {
            File file = new File(filePath);
            TLVector<User> users = new TLVector<>();
            if (file.exists()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
                String line;
                int readLines = 0;
                while (readLines < inviteLimitPerAccount && (line = br.readLine()) != null) {
                    if (!line.isEmpty()) {
                        readLines++;
                        String[] split = line.split(",");
                        int userId = Integer.parseInt(split[0]);
                        long userAccessHash = Long.parseLong(split[1]);
                        String userName = split[2];
                        User user = new User();
                        user.setAccessHash(userAccessHash);
                        user.setId(userId);
                        user.setUsername(userName);
                        users.add(user);
                    }
                }
                br.close();
            }

            //one by one
            List<User> invited = new ArrayList<>();
            for (User user : users) {
                boolean succeed = false;
                String errorCode = "";
                String errorTag = "";
                TimeUnit.MILLISECONDS.sleep(inviteIntervalMs);
                try {
                    if (inviteSelfContact) {
                        inviteUserToChannel(channelId, user.toTlInputUser());
                    } else {
                        inviteUserToChannel(channelId, user.resolveUsername(api).toTlInputUser());
                    }
                    succeed = true;
                } catch (TimeoutException e) {
                    log.error("invite timeout ", e);
                } catch (RpcException e) {
                    errorTag = e.getErrorTag();
                } catch (IOException e) {
                    log.error("invite error ", e);
                } catch (Exception e) {
                    log.error("unknown error ", e);
                }
                invited.add(user);
                log.info("{}! {} {}. {} added id({}) username({}) ",
                        succeed ? "successful" : "failed",
                        errorCode, errorTag, me.getId(), user.getId(), user.getUsername());
                log.info("invite process: {}/{} ", invited.size(), users.size());
                if (succeed && !notifications) {
                    clearAddUserMessage(channelId, 5);
                }
                if (errorTag.toUpperCase().contains("FLOOD")) {
                    log.error("account({}) FLOOD. {}", me.getId(), errorTag);
                    break;
                } else {
                    outputContactBlacklist(user.getId(), errorTag);
                }
            }
            //pageable
//            inviteUserToChannelPageable(channelId, users, invitePageSize);
        } catch (Exception e) {
            log.error("unknown error", e);
        }
    }

    @Deprecated
    private  void inviteUserToChannelPageable(int channelId, TLVector<TLAbsInputUser> users, int pageSize) throws IOException, TimeoutException {
        TLChannel channelTo = (TLChannel) chatsHashMap.get(channelId);
        TLInputChannel inputChannelTo = new TLInputChannel();
        inputChannelTo.setChannelId(channelTo.getId());
        inputChannelTo.setAccessHash(channelTo.getAccessHash());
        List<TLAbsInputUser> invited = new ArrayList<>();
        while (invited.size() < users.size()) {
            if (users.size() < invited.size() + pageSize) {
                pageSize = users.size() - invited.size();
            }
            List<TLAbsInputUser> slice = users.subList(invited.size(), invited.size() + pageSize);
            TLVector<TLAbsInputUser> sub = new TLVector<>();
            sub.addAll(slice);
            ChannelMethods.inviteUsers(api, inputChannelTo, sub);
            invited.addAll(slice);

            if (!notifications) {
                clearAddUserMessage(channelId, 5);
            }
            log.info("{} added  {}", me.getId(), JSON.toJSONString(sub));
            log.info("invite process: {}/{} ", invited.size(), users.size());
        }
    }

    public void outputUserDialog() {
        // output user dialogs
        for (TLDialog dialog : dialogs) {
            String fileNameFormat = "./dialogs/%s.dialog";
            String record = ConsoleOutputMethods.getDialogFullNameWithID(dialog.getPeer().getId(), chatsHashMap, usersHashMap) + "\n";
            FileMethods.appendBytesToFile(String.format(fileNameFormat, me.getPhone()), record.getBytes());
        }
    }


    public void outputChannelContact(int channelId) {
        int offset = 0;
        int limit = 200;
        int count = 1;
        List<Integer> users = new TLVector<>();
        while (users.size() < count && users.size() < 10000) { //api 只能拉取前1000个人，测试出来的
            TLChannelParticipants participants = getChannelUsers(channelId, offset, limit);
            count = participants.getCount();
            for (TLAbsUser u : participants.getUsers()) {
                TLUser user = (TLUser) u;
                String record = getUserString(user);
                String fileNameFormat = "./contacts/%s_channel_%s.contact";
                FileMethods.appendBytesToFile(String.format(fileNameFormat, me.getPhone(), channelId), record.getBytes());
            }
            List<Integer> ids = participants.getUsers().stream().map(m -> m.getId()).collect(Collectors.toList());
            users.addAll(ids);
            offset = users.size();
            log.info("output contact process :channel({}) {}/{} ", channelId, users.size(), count);
        }
    }

    public void outputChannelContactDiff(int sourceChannelId, int targetChannelId) {
        TLVector<TLAbsUser> source = getChannelAllUsers(sourceChannelId);
        log.info("get source({}) channel users finish size:{}", sourceChannelId, source.size());
        TLVector<TLAbsUser> target = getChannelAllUsers(targetChannelId);
        log.info("get target({}) channel users finish size:{}", targetChannelId, target.size());
        Set<Integer> sourceIds = source.stream().map(m -> m.getId()).collect(Collectors.toSet());
        Set<Integer> targetIds = target.stream().map(m -> m.getId()).collect(Collectors.toSet());
        //remove target
        sourceIds.removeAll(targetIds);
        //remove blacklist
        sourceIds.removeAll(blackListMap.keySet());
        String fileNameFormat = "./contacts/%s_channel_%s_rm_%s.contact";
        String fileName = String.format(fileNameFormat, me.getPhone(), sourceChannelId, targetChannelId);
        source.stream()
                .filter(f -> sourceIds.contains(f.getId()))
                .forEach(f -> {
                    TLUser user = (TLUser) f;
                    String record = getUserString(user);
                    FileMethods.appendBytesToFile(fileName, record.getBytes());
                });

    }

    private void outputContactBlacklist(Integer userId, String error) {
        String record = userId + "," + error + "\n";
        FileMethods.appendBytesToFile(blackListFilePath, record.getBytes());
    }


    private TLChannelParticipants getChannelUsers(int channelId, int offset, int limit) {
        TLChannel channelFrom = (TLChannel) chatsHashMap.get(channelId);
        TLInputChannel inputChannelFrom = new TLInputChannel();
        inputChannelFrom.setChannelId(channelFrom.getId());
        inputChannelFrom.setAccessHash(channelFrom.getAccessHash());
        return ChannelMethods.getUsers(api, inputChannelFrom, offset, limit);
    }

    private TLVector<TLAbsUser> getChannelAllUsers(int channelId) {
        TLChannel channelFrom = (TLChannel) chatsHashMap.get(channelId);
        TLInputChannel inputChannelFrom = new TLInputChannel();
        inputChannelFrom.setChannelId(channelFrom.getId());
        inputChannelFrom.setAccessHash(channelFrom.getAccessHash());
        return ChannelMethods.getAllUsers(api, inputChannelFrom);
    }

    private void inviteUserToChannel(int channelId, TLAbsInputUser user) throws IOException, TimeoutException {
        TLChannel channelTo = (TLChannel) chatsHashMap.get(channelId);
        TLInputChannel inputChannelTo = new TLInputChannel();

        inputChannelTo.setChannelId(channelTo.getId());
        inputChannelTo.setAccessHash(channelTo.getAccessHash());

        ChannelMethods.inviteUser(api, inputChannelTo, user);
    }

    public void clearAddUserMessage(int channelId, int limit) {
        TLChannel channelTo = (TLChannel) chatsHashMap.get(channelId);
        TLInputChannel inputChannelTo = new TLInputChannel();
        inputChannelTo.setChannelId(channelTo.getId());
        inputChannelTo.setAccessHash(channelTo.getAccessHash());
        Set<TLAbsMessage> history = MessageMethods.getHistory(api, inputChannelTo, limit);
        if (history != null && !history.isEmpty()) {
            Set<Integer> ids = history.stream().map(m -> ((TLMessageService) m).getId()).collect(Collectors.toSet());
            MessageMethods.deleteMessage(api, inputChannelTo, ids);
        }
    }

    @NotNull
    private static String getUserString(TLUser user) {
        return user.getId() + "," + user.getAccessHash() + "," +
                user.getUserName() + "," + user.getFirstName() + "," +
                user.getLastName() + "," + user.getPhone() + "," + user.getLangCode() + "\n";
    }


}
