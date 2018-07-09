package ru.myx.al.api.access;

class AuthTypeImplLoginNormal extends AuthTypeImpl {
	public static final String	AUTH_TYPE_LOGIN_NORMAL	= "login_normal";
	
	AuthTypeImplLoginNormal() {
		super( AuthTypeImplLoginNormal.AUTH_TYPE_LOGIN_NORMAL );
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String getUserId(final String uniqueId) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
