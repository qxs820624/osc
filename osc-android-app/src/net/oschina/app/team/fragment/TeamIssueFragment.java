package net.oschina.app.team.fragment;

import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

import net.oschina.app.AppContext;
import net.oschina.app.R;
import net.oschina.app.api.remote.OSChinaTeamApi;
import net.oschina.app.base.BaseActivity;
import net.oschina.app.base.BaseListFragment;
import net.oschina.app.bean.ListEntity;
import net.oschina.app.team.adapter.TeamIssueAdapter;
import net.oschina.app.team.bean.Team;
import net.oschina.app.team.bean.TeamIssueCatalog;
import net.oschina.app.team.bean.TeamIssue;
import net.oschina.app.team.bean.TeamIssueList;
import net.oschina.app.team.bean.TeamProject;
import net.oschina.app.team.ui.TeamMainActivity;
import net.oschina.app.ui.dialog.CommonDialog;
import net.oschina.app.ui.dialog.DialogHelper;
import net.oschina.app.ui.empty.EmptyLayout;
import net.oschina.app.util.StringUtils;
import net.oschina.app.util.UIHelper;
import net.oschina.app.util.XmlUtils;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * 任务列表界面
 * 
 * @author fireant(http://my.oschina.net/u/253900)
 * 
 */
public class TeamIssueFragment extends BaseListFragment<TeamIssue> {

    protected static final String TAG = TeamIssueFragment.class.getSimpleName();
    private static final String CACHE_KEY_PREFIX = "team_issue_list_";

    private String issueState = TeamIssue.TEAM_ISSUE_STATE_OPENED;
    
    private Team mTeam;

    private TeamProject mProject;

    private TeamIssueCatalog mCatalog;

    private int mTeamId;

    private int mProjectId;
    
    private int mCatalogId = -1;
    
    private boolean isNeedMenu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	Bundle bundle = getArguments();
	if (bundle != null) {
	    Team team = (Team) bundle
		    .getSerializable(TeamMainActivity.BUNDLE_KEY_TEAM);
	    if (team != null) {
		mTeam = team;
		mTeamId = StringUtils.toInt(mTeam.getId());
	    }
	    TeamProject project = (TeamProject) bundle
		    .getSerializable(TeamMainActivity.BUNDLE_KEY_PROJECT);
	    if (project != null) {
		this.mProject = project;
		this.mProjectId = project.getGit().getId();
	    } else {
		this.mProjectId = -1;
	    }
	    TeamIssueCatalog catalog = (TeamIssueCatalog) bundle.getSerializable(TeamMainActivity.BUNDLE_KEY_ISSUE_CATALOG);
	    if (catalog != null) {
		this.mCatalog = catalog;
		this.mCatalogId = catalog.getId();
		String title = catalog.getTitle() + "(" + catalog.getOpenedIssueCount() + "/" + catalog.getAllIssueCount() + ")";
		((BaseActivity)getActivity()).setActionBarTitle(title);
	    }
	    isNeedMenu = bundle.getBoolean("needmenu", true);
	}
	setHasOptionsMenu(isNeedMenu);
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // TODO Auto-generated method stub
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.team_issue_menu, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
	switch (item.getItemId()) {
	case R.id.team_new_issue:
	    UIHelper.showCreateNewIssue(getActivity(), mTeam, mProject, mCatalog);
	    break;
	case R.id.team_issue_change_state:
	    changeShowIssueState();
	    break;
	default:
	    break;
	}
        return super.onOptionsItemSelected(item);
    }
    
    private void changeShowIssueState() {
	final CommonDialog dialog = DialogHelper.getPinterestDialogCancelable(getActivity());
	CharSequence[] items = {"所有任务", "待办中", "进行中", "已完成", "已验收"};
	final CharSequence[] itemsEn = {"all", TeamIssue.TEAM_ISSUE_STATE_OPENED, TeamIssue.TEAM_ISSUE_STATE_UNDERWAY,
		TeamIssue.TEAM_ISSUE_STATE_CLOSED, TeamIssue.TEAM_ISSUE_STATE_ACCEPTED};
	dialog.setTitle("选择任务状态");
	int index = 0;
	for (int i = 0; i < itemsEn.length; i++) {
	    if (issueState.equals(itemsEn[i])) {
		index = i;
	    }
	}
	dialog.setItems(items, index ,new OnItemClickListener() {

	    @Override
	    public void onItemClick(AdapterView<?> parent, View view,
		    int position, long id) {
		// TODO Auto-generated method stub
		issueState = (itemsEn[position]).toString();
		
		onRefresh();
		dialog.dismiss();
	    }
	});
	dialog.show();
    }

    @Override
    protected TeamIssueAdapter getListAdapter() {
	return new TeamIssueAdapter();
    }
    
    public TeamIssueCatalog getTeamIssueCatalog() {
	return this.mCatalog;
    }

    /**
     * 获取当前展示页面的缓存数据
     */
    @Override
    protected String getCacheKeyPrefix() {
	return CACHE_KEY_PREFIX + mTeamId + "_" + mProjectId + "_" +  mCatalogId + "_"
		+ issueState;
    }

    @Override
    protected TeamIssueList parseList(InputStream is) throws Exception {
	TeamIssueList list = XmlUtils.toBean(TeamIssueList.class, is);
	return list;
    }

    @Override
    protected ListEntity<TeamIssue> readList(Serializable seri) {
	return ((TeamIssueList) seri);
    }

    @Override
    protected void executeOnLoadDataSuccess(List<TeamIssue> data) {
	super.executeOnLoadDataSuccess(data);
	if (mAdapter.getCount() == 1) {
	    setNoTeamIssue();
	}
    }

    private void setNoTeamIssue() {
	mErrorLayout.setErrorType(EmptyLayout.NODATA);
	mErrorLayout.setErrorImag(R.drawable.page_icon_empty);
	String msg = getResources().getString(R.string.team_empty_issue);
	mErrorLayout.setErrorMessage(msg);
    }

    @Override
    protected void sendRequestData() {
	int teamId = this.mTeamId;
	int projectId = this.mProjectId;
	int catalogId = mCatalogId;
	String source = mProject == null ? "" : mProject.getSource();
	int uid = mCatalogId == 0 ? 0 : AppContext.getInstance().getLoginUid();
	String scope = "";
	OSChinaTeamApi.getTeamIssueList(teamId, projectId, catalogId, source, uid, issueState,
		scope, mCurrentPage, AppContext.PAGE_SIZE, mHandler);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
	    long id) {
	TeamIssue issue = (TeamIssue) mAdapter.getItem(position);
	if (issue != null) {
	    UIHelper.showTeamIssueDetail(getActivity(), mTeam, issue, mCatalog);
	}
    }
    
    @Override
    protected long getAutoRefreshTime() {
        // TODO Auto-generated method stub
	// 1小时间距，主动刷新列表
        return 1 * 60 * 60;
    }
}