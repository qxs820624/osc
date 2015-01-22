package net.oschina.app.team.fragment;

import java.io.InputStream;
import java.io.Serializable;

import net.oschina.app.AppContext;
import net.oschina.app.api.remote.OSChinaTeamApi;
import net.oschina.app.base.BaseListFragment;
import net.oschina.app.base.ListBaseAdapter;
import net.oschina.app.bean.ListEntity;
import net.oschina.app.team.adapter.TeamIssueAdapter;
import net.oschina.app.team.bean.Team;
import net.oschina.app.team.bean.TeamIssue;
import net.oschina.app.team.bean.TeamIssueList;
import net.oschina.app.team.bean.TeamProject;
import net.oschina.app.team.ui.TeamMainActivity;
import net.oschina.app.util.StringUtils;
import net.oschina.app.util.XmlUtils;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

/**
 * 任务列表界面
 * 
 * @author fireant(http://my.oschina.net/u/253900)
 *
 */
public class TeamIssueFragment extends BaseListFragment {
	
    protected static final String TAG = TeamIssueFragment.class.getSimpleName();
    private static final String CACHE_KEY_PREFIX = "team_issue_list_";
    
    private Team mTeam;
    
    private TeamProject mProject;
    
    private int mTeamId;
    
    private int mProjectId;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
        	Team team = (Team) bundle.getSerializable(TeamMainActivity.BUNDLE_KEY_TEAM);
        	if (team != null) {
        		mTeam = team;
        		mTeamId = StringUtils.toInt(mTeam.getId());
        	}
        	TeamProject project = (TeamProject) bundle.getSerializable(TeamMainActivity.BUNDLE_KEY_PROJECT);
        	if (project != null) {
        		this.mProject = project;
        		this.mProjectId = project.getGit().getId();
        	}
        }
    }

    @Override
    protected ListBaseAdapter getListAdapter() {
        return new TeamIssueAdapter();
    }

	/**
     * 获取当前展示页面的缓存数据
     */
    @Override
    protected String getCacheKeyPrefix() {
        return CACHE_KEY_PREFIX + mTeamId + "_" + mProjectId + "_" + mCurrentPage;
    }

	@Override
    protected ListEntity<TeamIssue> parseList(InputStream is) throws Exception {
    	TeamIssueList<TeamIssue> list = XmlUtils.toBean(TeamIssueList.class, is);
        return list;
    }

    @Override
    protected ListEntity<TeamIssue> readList(Serializable seri) {
        return ((TeamIssueList) seri);
    }

    @Override
    protected void sendRequestData() {
    	String source = mProject == null ? "" : mProject.getSource();
    	AppContext.showToast(source);
    	OSChinaTeamApi.getTeamIssueList(mTeamId, mProjectId, source, 0, "all", "", mCurrentPage, AppContext.PAGE_SIZE, mHandler);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
       
    }
    
}
