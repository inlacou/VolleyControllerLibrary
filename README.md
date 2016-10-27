# VolleyControllerLibrary

[![](https://jitpack.io/v/inlacou/VolleyControllerLibrary.svg)](https://jitpack.io/#inlacou/VolleyControllerLibrary)

Initialize the library on your Application class:

```java
@Override
	public void onCreate() {
		super.onCreate();
		
		// initialize
		NetworkLogic.getInstance().init(this, new NetworkLogic.LogicCallbacks() {
			@Override
			public void setTokens(String authToken, String refreshToken) {
				//Save authToken
        //Save refreshToken
			}

			@Override
			public String getRefreshToken() {
				//get refreshToken
        return savedRefreshToken;
			}

			@Override
			public String getAuthToken() {
				//get authToken
				return savedAuthToken;
			}

			@Override
			public void doRefreshToken(NetworkLogic.IOCallbacks ioCallbacks) {
				//make call to refresh token, for example:
				UrlLogic.doPostRefreshToken(ioCallbacks);
			}
		});
	}
```

This calls allow the library to listen for authToken expiration and handle it's refresh. If you make several calls at once and all file because the authToken has expired, `LogicCallbacks.doRefreshToken(IOCallbacks ioCallbacks)` will be called and then all these calls will be remade with the new authToken.

It also checks for `refreshToken` invalid or expired message, in which case it does not save and resume calls. It hands you a callback so you can, for example, close user session.

Library asks you for the messages to check this states. For `authToken`, it also checks for these messages by default:

`The access token provided has expired`
`The access token provided is invalid`
`UnauthorizedError: jwt expired`

Example call:

```java
public static void doPutUserData(String name, String lastname,
                                     long birthdate,
                                     int userSex, int pc,
                                     String code,
                                     NetworkLogic.IOCallbacks callback){
        HashMap<String, String> map = new HashMap();
        map.put("name", name);
        map.put("lastname", lastname);
        map.put("birthdate", birthdate+"");
        map.put("userSex", userSex+"");
        map.put("pc", pc+"");
	
        NetworkLogic.getInstance().doPut(baseUrl + "/api/users/me?access_token=" + SharedPreferencesManager.getAuthToken(), map, code, callback);
    }
```

Maybe you are confused by the `code` param. It's just for easier debugging, you can use empty Strings without problem.

There you go!
