/*
 * Title: JkMain.java
 * Project: Jawlergram
 * Creator: Georgii Mikriukov
 * 2019
 */

package com.crawlergram.crawler;

import com.crawlergram.crawler.apicallback.ApiCallbackImplemented;
import com.crawlergram.crawler.apimethods.AuthMethods;
import com.crawlergram.crawler.apimethods.ChannelMethods;
import com.crawlergram.crawler.apimethods.DialogsHistoryMethods;
import com.crawlergram.crawler.logs.LogMethods;
import com.crawlergram.db.DBStorage;
import lombok.extern.slf4j.Slf4j;
import org.telegram.api.chat.TLAbsChat;
import org.telegram.api.chat.channel.TLChannel;
import org.telegram.api.dialog.TLDialog;
import org.telegram.api.engine.ApiCallback;
import org.telegram.api.engine.AppInfo;
import org.telegram.api.engine.TelegramApi;
import org.telegram.api.engine.storage.AbsApiState;
import org.telegram.api.input.chat.TLInputChannel;
import org.telegram.api.input.user.TLAbsInputUser;
import org.telegram.api.input.user.TLInputUser;
import org.telegram.api.message.TLAbsMessage;
import org.telegram.api.user.TLAbsUser;
import org.telegram.api.user.TLUser;
import org.telegram.bot.kernel.engine.MemoryApiState;
import org.telegram.tl.TLVector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * created by jacky. 2019/3/19 5:11 PM
 */
@Slf4j
public class DBMain {

    // api variables
    private static int APIKEY = 747275; // your api keys
    private static String APIHASH = "22eca58b2a2b110e972a050f7f2cb2ac"; // your api hash
    private static String PHONENUMBER = "+8618691718911"; // your phone number
    private static String API_STATE_FILE = PHONENUMBER+".session"; // api state is saved to HDD
    private static String DEVICE_MODEL = "PC"; // model name
    private static String OS = "test"; // os name
    private static String VERSION = "1"; // version
    private static String LANG_CODE = "en"; // language code
    private static String NAME = "John"; // name (for signing up)
    private static String SURNAME = "Doe"; // surname (for signing up)


    // other
    private static AbsApiState apiState;
    private static AppInfo appInfo;
    private static ApiCallback apiCallback = new ApiCallbackImplemented();
    private static TelegramApi api;
    private static Map<Integer, TLAbsChat> chatsHashMap = new HashMap<>();
    private static Map<Integer, TLAbsUser> usersHashMap = new HashMap<>();
    private static TLVector<TLDialog> dialogs = new TLVector<>();
    private static Map<Integer, TLAbsMessage> messagesHashMap = new HashMap<>();
    private static DBStorage dbStorage;

    public static void main(String[] args) {
        initApiDoAuth();
        System.out.println("finish");
    }


    private static void initApiDoAuth() {

        //register loggers (registration is preferable, otherwise - output will be in console)
        LogMethods.registerLogs("logs", false);

        // api state
        apiState = new MemoryApiState(API_STATE_FILE);
        apiState.setPrimaryDc(5);

        // app info set
        appInfo = new AppInfo(APIKEY, DEVICE_MODEL, OS, VERSION, LANG_CODE);

        // init api
        api = new TelegramApi(apiState, appInfo, apiCallback);

        // set api state
        AuthMethods.setApiState(api, apiState);

        // do auth
        AuthMethods.auth(api, apiState, APIKEY, APIHASH, PHONENUMBER, NAME, SURNAME);

//        // get all dialogs of user (telegram returns 100 dialogs at maximum, getting by slices)
        DialogsHistoryMethods.getDialogsChatsUsers(api, dialogs, chatsHashMap, usersHashMap, messagesHashMap);
//
        // output user dialogs
//        for (TLDialog dialog : dialogs) {
//            System.out.println(ConsoleOutputMethods.getDialogFullNameWithID(dialog.getPeer().getId(), chatsHashMap, usersHashMap));
//        }

        TLVector<TLAbsUser> users = getChannelUsers(1157009326);
//        inviteUserToChannel(1405149694, 679560412);


        System.out.println("Asdfas");
    }

    private static void inviteUserToChannel(int channelId, int userId) {
        //        TLChannel channelTo = (TLChannel) chatsHashMap.get(1157009326);//Dipbit_official
        TLChannel channelTo = (TLChannel) chatsHashMap.get(channelId); //Dipbit中文群
        TLInputChannel inputChannelTo = new TLInputChannel();
        inputChannelTo.setChannelId(channelTo.getId());
        inputChannelTo.setAccessHash(channelTo.getAccessHash());


        TLUser u = (TLUser) usersHashMap.get(userId); //孟辉
//        TLUser u = (TLUser) usersHashMap.get(761201490); //叫爸爸
        TLInputUser user = new TLInputUser();
        user.setUserId(u.getId());
        user.setAccessHash(u.getAccessHash());
        ChannelMethods.inviteUser(api, inputChannelTo, user);
    }


    public static void getVERSION() {
        TLVector<TLAbsUser> users = getChannelUsers(1157009326);


        TLChannel channelTo = (TLChannel) chatsHashMap.get(1157009326);
        TLInputChannel inputChannelTo = new TLInputChannel();
        inputChannelTo.setChannelId(channelTo.getId());


        List<TLInputUser> inputUserList = users.stream().map(u -> {
            TLInputUser input = new TLInputUser();
            input.setUserId(u.getId());
            input.setAccessHash(((TLUser) u).getAccessHash());
            return input;
        }).collect(Collectors.toList());
        TLVector<TLAbsInputUser> inputUsers = new TLVector<>();
        inputUsers.addAll(inputUserList);
        ChannelMethods.inviteUsers(api, inputChannelTo, inputUsers);
    }

    private static TLVector<TLAbsUser> getChannelUsers(int channelId) {
        TLChannel channelFrom = (TLChannel) chatsHashMap.get(channelId);
        TLInputChannel inputChannelFrom = new TLInputChannel();
        inputChannelFrom.setChannelId(channelFrom.getId());
        inputChannelFrom.setAccessHash(channelFrom.getAccessHash());
        return ChannelMethods.getUsers(api, inputChannelFrom);
    }
}
