package net.oschina.app.fragment;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;

import org.apache.http.Header;

import butterknife.ButterKnife;
import butterknife.InjectView;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import net.oschina.app.AppContext;
import net.oschina.app.R;
import net.oschina.app.api.remote.OSChinaApi;
import net.oschina.app.base.BaseFragment;
import net.oschina.app.bean.Constants;
import net.oschina.app.bean.MyInformation;
import net.oschina.app.bean.Notice;
import net.oschina.app.bean.User;
import net.oschina.app.cache.CacheManager;
import net.oschina.app.ui.MainActivity;
import net.oschina.app.ui.empty.EmptyLayout;
import net.oschina.app.util.StringUtils;
import net.oschina.app.util.TDevice;
import net.oschina.app.util.UIHelper;
import net.oschina.app.util.XmlUtils;
import net.oschina.app.widget.AvatarView;
import net.oschina.app.widget.BadgeView;

/**
 * 登录用户中心页面
 * @author FireAnt（http://my.oschina.net/LittleDY）
 * @version 创建时间：2014年10月30日 下午4:05:47 
 * 
 */

public class MyInformationFragment extends BaseFragment {
	
	@InjectView(R.id.iv_avatar)AvatarView mIvAvatar;
	@InjectView(R.id.iv_gender)ImageView mIvGender;
	@InjectView(R.id.tv_name) TextView mTvName;
	@InjectView(R.id.tv_sigin) TextView mTvSign;
	@InjectView(R.id.tv_favorite) TextView mTvFavorite;
	@InjectView(R.id.tv_following) TextView mTvFollowing;
	@InjectView(R.id.tv_follower) TextView mTvFollower;
	@InjectView(R.id.tv_mes) View mMesView;
//	@InjectView(R.id.tv_join_time) TextView mTvJoinTime;
//	@InjectView(R.id.tv_location) TextView mTvLocation;
//	@InjectView(R.id.tv_development_platform) TextView mTvDevelopmentPlatform;
//	@InjectView(R.id.tv_academic_focus) TextView mTvAcademicFocus;
	@InjectView(R.id.error_layout) EmptyLayout mErrorLayout;
	
	private static BadgeView mMesCount;
	
	private boolean mIsWatingLogin;
	
	private User mInfo;
	private AsyncTask<String, Void, User> mCacheTask;
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(Constants.INTENT_ACTION_LOGOUT)) {
				if (mErrorLayout != null) {
					mIsWatingLogin = true;
					mErrorLayout.setErrorType(EmptyLayout.NETWORK_ERROR);
					mErrorLayout.setErrorMessage(getString(R.string.unlogin_tip));
				}
			} else if (action.equals(Constants.INTENT_ACTION_USER_CHANGE)) {
				requestData(true);
			} else if (action.equals(Constants.INTENT_ACTION_NOTICE)) {
				setNotice();
			}
		}
	};

	private AsyncHttpResponseHandler mHandler = new AsyncHttpResponseHandler() {

		@Override
		public void onSuccess(int arg0, Header[] arg1, byte[] arg2) {
			try {
				mInfo = XmlUtils.toBean(MyInformation.class, new ByteArrayInputStream(arg2)).getUser();
				if (mInfo != null) {
					fillUI();
					mErrorLayout.setErrorType(EmptyLayout.HIDE_LAYOUT);
					new SaveCacheTask(getActivity(), mInfo, getCacheKey())
							.execute();
				} else {
					onFailure(arg0, arg1, arg2, new Throwable());
				}
			} catch (Exception e) {
				e.printStackTrace();
				onFailure(arg0, arg1, arg2, e);
			}
		}

		@Override
		public void onFailure(int arg0, Header[] arg1, byte[] arg2,
				Throwable arg3) {
			mErrorLayout.setErrorType(EmptyLayout.NETWORK_ERROR);
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		IntentFilter filter = new IntentFilter(Constants.INTENT_ACTION_LOGOUT);
		filter.addAction(Constants.INTENT_ACTION_USER_CHANGE);
		filter.addAction(Constants.INTENT_ACTION_NOTICE);
		getActivity().registerReceiver(mReceiver, filter);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		setNotice();
	}
	
	private void setNotice() {
		if (MainActivity.mNotice != null) {

			Notice notice = MainActivity.mNotice;
			int atmeCount = notice.getAtmeCount();// @我
			int msgCount = notice.getMsgCount();// 留言
			int reviewCount = notice.getReviewCount();// 评论
			int newFansCount = notice.getNewFansCount();// 新粉丝
			int activeCount = atmeCount + reviewCount + msgCount + newFansCount;// 信息总数

			if (activeCount > 0) {
				mMesCount.setText(activeCount + "");
				mMesCount.show();
			} else {
				mMesCount.hide();
			}

		} else {
			mMesCount.hide();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		getActivity().unregisterReceiver(mReceiver);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_my_information, container,
				false);
		ButterKnife.inject(this, view);
		initView(view);
		requestData(false);
		return view;
	}
	
	@Override
	public void initView(View view) {
		mErrorLayout.setErrorType(EmptyLayout.HIDE_LAYOUT);
		mIvAvatar.setOnClickListener(this);
		mErrorLayout.setOnLayoutClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (AppContext.getInstance().isLogin()) {
					requestData(true);
				} else {
					UIHelper.showLoginActivity(getActivity());
				}
			}
		});
		view.findViewById(R.id.rl_user_center).setOnClickListener(this);
		view.findViewById(R.id.ly_favorite).setOnClickListener(this);
		view.findViewById(R.id.ly_following).setOnClickListener(this);
		view.findViewById(R.id.ly_follower).setOnClickListener(this);
		view.findViewById(R.id.rl_message).setOnClickListener(this);
		view.findViewById(R.id.rl_team).setOnClickListener(this);
		view.findViewById(R.id.rl_blog).setOnClickListener(this);
		view.findViewById(R.id.rl_note).setOnClickListener(this);
		
		mMesCount = new BadgeView(getActivity(), mMesView);
		mMesCount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
		mMesCount.setBadgePosition(BadgeView.POSITION_CENTER);
		mMesCount.setGravity(Gravity.CENTER);
		mMesCount.setBackgroundResource(R.drawable.notification_bg);
	}

	private void fillUI() {
		ImageLoader.getInstance().displayImage(
				AvatarView.getLargeAvatar(mInfo.getPortrait()), mIvAvatar);
		mTvName.setText(mInfo.getName());
		mIvGender
				.setImageResource(StringUtils.toInt(mInfo.getGender()) == 1 ? R.drawable.userinfo_icon_male
						: R.drawable.userinfo_icon_female);
		mTvFavorite.setText(String.valueOf(mInfo.getFavoritecount()));
		mTvFollowing.setText(String.valueOf(mInfo.getFollowerscount()));
		mTvFollower.setText(String.valueOf(mInfo.getFanscount()));

//		mTvJoinTime.setText(mInfo.getJointime());
//		mTvLocation.setText(mInfo.getFrom());
		mTvSign.setText(mInfo.getFrom());
//		mTvDevelopmentPlatform.setText(mInfo.getDevplatform());
//		mTvAcademicFocus.setText(mInfo.getExpertise());
	}
	
	private void requestData(boolean refresh) {
		
		if (AppContext.getInstance().isLogin()) {
			mIsWatingLogin = false;
			String key = getCacheKey();
			if (TDevice.hasInternet()
					&& (!CacheManager.isReadDataCache(getActivity(), key) || refresh)) {
				sendRequestData();
			} else {
				readCacheData(key);
			}
		} else {
			mIsWatingLogin = true;
			mErrorLayout.setErrorType(EmptyLayout.NETWORK_ERROR);
			mErrorLayout.setErrorMessage(getString(R.string.unlogin_tip));
		}
	}

	private void readCacheData(String key) {
		cancelReadCacheTask();
		mCacheTask = new CacheTask(getActivity()).execute(key);
	}

	private void cancelReadCacheTask() {
		if (mCacheTask != null) {
			mCacheTask.cancel(true);
			mCacheTask = null;
		}
	}

	private void sendRequestData() {
		int uid = AppContext.getInstance().getLoginUid();
		OSChinaApi.getMyInformation(uid, mHandler);
	}

	private String getCacheKey() {
		return "my_information" + AppContext.getInstance().getLoginUid();
	}

	private class CacheTask extends AsyncTask<String, Void, User> {
		private WeakReference<Context> mContext;

		private CacheTask(Context context) {
			mContext = new WeakReference<Context>(context);
		}

		@Override
		protected User doInBackground(String... params) {
			Serializable seri = CacheManager.readObject(mContext.get(),
					params[0]);
			if (seri == null) {
				return null;
			} else {
				return (User) seri;
			}
		}

		@Override
		protected void onPostExecute(User info) {
			super.onPostExecute(info);
			mInfo = info;
			if (mInfo != null) {
				fillUI();
				mErrorLayout.setErrorType(EmptyLayout.HIDE_LAYOUT);
			} else {
				mErrorLayout.setErrorType(EmptyLayout.NETWORK_ERROR);
			}
		}
	}

	private class SaveCacheTask extends AsyncTask<Void, Void, Void> {
		private WeakReference<Context> mContext;
		private Serializable seri;
		private String key;

		private SaveCacheTask(Context context, Serializable seri, String key) {
			mContext = new WeakReference<Context>(context);
			this.seri = seri;
			this.key = key;
		}

		@Override
		protected Void doInBackground(Void... params) {
			CacheManager.saveObject(mContext.get(), seri, key);
			return null;
		}
	}
	@Override
	public void onClick(View v) {
		final int id = v.getId();
		switch (id) {
		case R.id.iv_avatar:
			UIHelper.showUserAvatar(getActivity(), mInfo.getPortrait());
			break;
		case R.id.ly_follower:
			UIHelper.showFriends(getActivity(), AppContext.getInstance().getLoginUid(), 1);
			break;
		case R.id.ly_following:
			UIHelper.showFriends(getActivity(), AppContext.getInstance().getLoginUid(), 0);
			break;
		case R.id.ly_favorite:
			UIHelper.showUserFavorite(getActivity(),AppContext.getInstance().getLoginUid());
			break;
		case R.id.rl_message:
			UIHelper.showMyMes(getActivity());
			break;
		case R.id.rl_team:
			
			break;
		case R.id.rl_blog:
			UIHelper.showUserBlog(getActivity(), AppContext.getInstance().getLoginUid());
			break;
		case R.id.rl_note:
			
			break;
		case R.id.rl_user_center:
			UIHelper.showUserCenter(getActivity(), AppContext.getInstance().getLoginUid(), mInfo.getName());
			break;
		default:
			break;
		}
	}

	@Override
	public void initData() {

	}
}