import com.crawlergram.crawler.apicallback.ApiCallbackImplemented;
import com.crawlergram.crawler.apimethods.AuthMethods;
import com.crawlergram.crawler.apimethods.DialogsHistoryMethods;
import com.crawlergram.crawler.apimethods.MediaDownloadMethods;
import com.crawlergram.crawler.logs.LogMethods;
import com.crawlergram.crawler.output.ConsoleOutputMethods;
import org.telegram.api.chat.TLAbsChat;
import org.telegram.api.dialog.TLDialog;
import org.telegram.api.engine.ApiCallback;
import org.telegram.api.engine.AppInfo;
import org.telegram.api.engine.TelegramApi;
import org.telegram.api.engine.storage.AbsApiState;
import org.telegram.api.message.TLAbsMessage;
import org.telegram.api.user.TLAbsUser;
import org.telegram.bot.kernel.engine.MemoryApiState;
import org.telegram.tl.TLVector;

import java.util.HashMap;

public class VoiceMessagesExtractor {

    private static final int APIKEY = 0; // your api keys
    private static final String APIHASH = ""; // your api hash
    private static final String PHONENUMBER = ""; // your phone number

    public static void main(String[] args) throws InterruptedException {

        long time0 = System.currentTimeMillis();

        //register loggers
        LogMethods.registerLogs("logs", false);

        // api state
        AbsApiState apiState = new MemoryApiState("api.state");

        // app info set
        AppInfo appInfo = new AppInfo(APIKEY, "desktop", "Windows", "pre alpha 0.01", "en");

        // api callback methods
        ApiCallback apiCallback = new ApiCallbackImplemented();

        // init api
        TelegramApi api = new TelegramApi(apiState, appInfo, apiCallback);

        // set api state
        AuthMethods.setApiState(api, apiState);

        // do auth
        AuthMethods.auth(api, apiState, APIKEY, APIHASH, PHONENUMBER, "John", "Doe");

        // dialogs, chats, users structures
        HashMap<Integer, TLAbsChat> chatsHashMap = new HashMap<>();
        HashMap<Integer, TLAbsUser> usersHashMap = new HashMap<>();
        TLVector<TLDialog> dialogs = new TLVector<>();
        //hashmap with top messages (needed for offsets)
        HashMap<Integer, TLAbsMessage> messagesHashMap = new HashMap<>();

        // get all dialogs of user (telegram returns 100 dialogs at maximum, getting by slices)
        DialogsHistoryMethods.getDialogsChatsUsers(api, dialogs, chatsHashMap, usersHashMap, messagesHashMap);

        // output to console
        ConsoleOutputMethods.testChatsHashMapOutputConsole(chatsHashMap);
        ConsoleOutputMethods.testUsersHashMapOutputConsole(usersHashMap);


        int msgLimit = 20000;

        TLDialog dialog = new TLDialog();

        for (TLDialog d: dialogs){
            if (d.getPeer().getId() == 1155181743){ //415770675
                dialog = d;
            }
        }

        System.out.println(dialog.getTopMessage());


        TLAbsMessage topMessage = DialogsHistoryMethods.getTopMessage(dialog, messagesHashMap);
        TLVector<TLAbsMessage> absMessages = DialogsHistoryMethods.getWholeMessageHistory(api, dialog, chatsHashMap, usersHashMap, topMessage, msgLimit, 0, 0);

        int filesCounter = 0;

        long time1 = System.currentTimeMillis();

        for (TLAbsMessage absMessage : absMessages) {
            if (MediaDownloadMethods.messageDownloadVoiceMessagesToHDD(api, absMessage, 8 * 5 * 1024 * 1024, "files") != null){
                filesCounter++;
                Thread.sleep(100);
            }
        }

        long time2 = System.currentTimeMillis();

        System.out.println();
        System.out.println("Messages downloaded: " + absMessages.size());
        System.out.println("Voice messages: " + filesCounter);
        System.out.println("Time to download messages: " + (time1-time0));
        System.out.println("Time to download audios: " + (time2-time1));

        System.exit(0);
    }
}
