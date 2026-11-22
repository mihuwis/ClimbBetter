import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

/** Local operator inspection. Prints no keys, access tokens or email addresses. */
public class InspectFirebaseAccounts {
    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !args[0].equals("climbbetter-dev")) {
            throw new IllegalArgumentException("Usage: InspectFirebaseAccounts climbbetter-dev <credentials-file>");
        }
        FirebaseOptions options;
        try (var input = new FileInputStream(args[1])) {
            options = FirebaseOptions.builder().setProjectId(args[0])
                    .setCredentials(GoogleCredentials.fromStream(input)).build();
        }
        FirebaseApp app = FirebaseApp.initializeApp(options);
        try {
            var accounts = new ArrayList<Object>();
            var page = FirebaseAuth.getInstance(app).listUsers(null, 10);
            for (var user : page.getValues()) {
                var item = new LinkedHashMap<String,Object>();
                item.put("uid", user.getUid());
                item.put("displayName", user.getDisplayName());
                item.put("providers", Arrays.stream(user.getProviderData()).map(provider -> provider.getProviderId()).toList());
                item.put("lastSignInTimestamp", user.getUserMetadata().getLastSignInTimestamp());
                accounts.add(item);
            }
            System.out.println(new Gson().toJson(java.util.Map.of("projectId", args[0], "accounts", accounts,
                    "hasMore", page.hasNextPage())));
        } finally {
            app.delete();
        }
    }
}
