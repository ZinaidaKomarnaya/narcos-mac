package ru.gg.lib_gwt;

public class Const {

public static final String ANDROID_APP = "com.ftxgames.narcos";
public static final String TRUE = "true";
public static final String FALSE = "false";
public static boolean ONLY_LOGIN = false;
public static final String ACTION = "action";
public static final String ANDROID_LOG_SERVER = "http://10.0.2.2:54322/log";
public static final String ANDROID_BREAK_SERVICE = "http://10.0.2.2:54322/break";
public static final String BREAK = "break";
public static final String ANDROID_BACK_SERVICE = "http://10.0.2.2:54322/back";
public static final String ANDROID_PAUSE_SERVICE = "http://10.0.2.2:54322/pause";

public static final String CLIPBOARD_COMMENT_TITLE = "http://10.0.2.2:54322/clipboard";
public static final String BACK = "back";
public static final String UTF_8 = "UTF-8";

public static class JreAction {
	public static enum Type {
		start,
		finish,
		failed,
		getProxy,
		getAccount,
		getComment;
	}

	public static abstract class BasicReq extends JsonBasic {

	}

	public static abstract class BasicResp extends JsonBasic {

	}

	public static class Start {
		public static class Request extends BasicReq {
		}
		public static class Response extends BasicResp {

		}
	}

	public static class GetAccount {
		public static class Request extends BasicReq {

		}
		public static class Response extends BasicResp {

		}
	}

	public static class GetComment {
		public static class Request extends BasicReq {

		}
		public static class Response extends BasicResp {

		}
	}

	public static class Failed {
		public static class Request extends BasicReq {

		}
		public static class Response extends BasicResp {

		}
	}

	public static class Finish {
		public static class Request extends BasicReq {

		}
		public static class Response extends BasicResp {

		}
	}

	public static class GetProxy {
		public static class Request extends BasicReq {
			public String region;
		}
		public static class Response extends BasicResp {

		}
	}

}

}
