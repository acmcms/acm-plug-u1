package ru.myx.al.api.access;

class AuthTypeImplEmail extends AuthTypeImpl {
	public static final String	AUTH_TYPE_EMAIL	= "email";
	
	AuthTypeImplEmail() {
		super( AuthTypeImplEmail.AUTH_TYPE_EMAIL );
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String getUserId(final String uniqueId) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
