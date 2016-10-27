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

There you go!
