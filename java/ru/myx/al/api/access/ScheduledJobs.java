/*
 * Created on 10.04.2006
 */
package ru.myx.al.api.access;

import ru.myx.ae3.act.Act;

final class ScheduledJobs implements Runnable {
	
	private final AccessManagerImpl manager;

	private boolean stopped = false;

	ScheduledJobs(final AccessManagerImpl manager) {
		
		this.manager = manager;
		Act.later(null, this, 15_000L);
	}

	@Override
	public void run() {
		
		try {
			{
				final UserData[] users;
				synchronized (this.manager.toCommitUsers) {
					if (this.manager.toCommitUsers.size() > 0) {
						users = this.manager.toCommitUsers.toArray(new UserData[this.manager.toCommitUsers.size()]);
						this.manager.toCommitUsers.clear();
					} else {
						users = null;
					}
				}
				if (users != null) {
					this.manager.scheduledCommitUserData(users);
				}
			}
			{
				final UserProfileData[] profiles;
				synchronized (this.manager.toCommitProfiles) {
					if (this.manager.toCommitProfiles.size() > 0) {
						profiles = this.manager.toCommitProfiles.toArray(new UserProfileData[this.manager.toCommitProfiles.size()]);
						this.manager.toCommitProfiles.clear();
					} else {
						profiles = null;
					}
				}
				if (profiles != null) {
					this.manager.scheduledCommitUserProfileData(profiles);
				}
			}
			{
				final String[] IDs;
				synchronized (this.manager.toUpdateUsers) {
					if (this.manager.toUpdateUsers.size() > 0) {
						IDs = this.manager.toUpdateUsers.toArray(new String[this.manager.toUpdateUsers.size()]);
						this.manager.toUpdateUsers.clear();
					} else {
						IDs = null;
					}
				}
				if (IDs != null) {
					this.manager.scheduledUpdateAccessedUsers(IDs);
				}
			}
			{
				UserProfileData[] profiles = null;
				synchronized (this.manager.toUpdateProfiles) {
					if (this.manager.toUpdateProfiles.size() > 0) {
						profiles = new UserProfileData[this.manager.toUpdateProfiles.size()];
						profiles = this.manager.toUpdateProfiles.toArray(profiles);
						this.manager.toUpdateProfiles.clear();
					}
				}
				if (profiles != null) {
					this.manager.scheduledUpdateAccessedProfiles(profiles);
				}
			}
		} finally {
			if (!this.stopped) {
				Act.later(null, this, 5_000L);
			}
		}
	}

	@Override
	public String toString() {
		
		return "AccessManager delayed tasks";
	}

	void stop() {
		
		this.stopped = true;
	}
}
