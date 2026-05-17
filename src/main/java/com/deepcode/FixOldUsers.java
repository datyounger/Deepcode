package com.deepcode;

import com.deepcode.dao.*;
import com.deepcode.model.*;
import com.deepcode.service.AIAnalyzer;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Fix users that have old submissions without source code (tourist, subnet, benq).
 * Updates their existing submissions with sample code and adds analysis results.
 */
public class FixOldUsers {

    static String[][] codeSamples = {
        {"Two Sum","GNU C++17","Two Pointers,Greedy","Array,HashMap","0.10","Time:O(n),Space:O(n)","Tim 2 so co tong bang target.",
         "#include<bits/stdc++.h>\nusing namespace std;\nint main(){int n,t;cin>>n>>t;unordered_map<int,int>mp;vector<int>a(n);for(int i=0;i<n;i++)cin>>a[i];for(int i=0;i<n;i++){if(mp.count(t-a[i])){cout<<mp[t-a[i]]<<\" \"<<i;return 0;}mp[a[i]]=i;}cout<<-1;}"},
        {"Dijkstra","GNU C++17","Dijkstra,Greedy","Graph,Priority Queue,Array","0.08","Time:O((V+E)logV)","Dijkstra tim duong di ngan nhat.",
         "#include<bits/stdc++.h>\nusing namespace std;\ntypedef pair<int,int>pii;int main(){int n,m,s,t;cin>>n>>m>>s>>t;vector<vector<pii>>g(n+1);for(int i=0;i<m;i++){int u,v,w;cin>>u>>v>>w;g[u].push_back({v,w});}vector<int>d(n+1,1e9);priority_queue<pii,vector<pii>,greater<pii>>pq;d[s]=0;pq.push({0,s});while(!pq.empty()){auto[c,u]=pq.top();pq.pop();if(c>d[u])continue;for(auto[v,w]:g[u])if(d[u]+w<d[v]){d[v]=d[u]+w;pq.push({d[v],v});}}cout<<d[t];}"},
        {"Knapsack DP","GNU C++17","Dynamic Programming","Array,2D Array","0.12","Time:O(nW),Space:O(nW)","Ba lo quy hoach dong.",
         "#include<bits/stdc++.h>\nusing namespace std;\nint main(){int n,W;cin>>n>>W;vector<int>w(n),v(n);for(int i=0;i<n;i++)cin>>w[i]>>v[i];vector<vector<int>>dp(n+1,vector<int>(W+1,0));for(int i=1;i<=n;i++)for(int j=0;j<=W;j++){dp[i][j]=dp[i-1][j];if(j>=w[i-1])dp[i][j]=max(dp[i][j],dp[i-1][j-w[i-1]]+v[i-1]);}cout<<dp[n][W];}"},
        {"BFS Grid","GNU C++17","BFS","Graph,Queue,2D Array","0.07","Time:O(NM),Space:O(NM)","BFS tren luoi.",
         "#include<bits/stdc++.h>\nusing namespace std;\nint dx[]={0,0,1,-1},dy[]={1,-1,0,0};int main(){int n,m;cin>>n>>m;vector<string>g(n);for(auto&s:g)cin>>s;vector<vector<int>>d(n,vector<int>(m,-1));queue<pair<int,int>>q;q.push({0,0});d[0][0]=0;while(!q.empty()){auto[x,y]=q.front();q.pop();for(int i=0;i<4;i++){int nx=x+dx[i],ny=y+dy[i];if(nx>=0&&nx<n&&ny>=0&&ny<m&&g[nx][ny]!='#'&&d[nx][ny]==-1){d[nx][ny]=d[x][y]+1;q.push({nx,ny});}}}cout<<d[n-1][m-1];}"},
        {"DFS Components","GNU C++17","DFS","Graph,Array","0.06","Time:O(V+E)","DFS dem thanh phan lien thong.",
         "#include<bits/stdc++.h>\nusing namespace std;\nvector<int>adj[200005];bool vis[200005];void dfs(int u){vis[u]=1;for(int v:adj[u])if(!vis[v])dfs(v);}int main(){int n,m;cin>>n>>m;for(int i=0;i<m;i++){int u,v;cin>>u>>v;adj[u].push_back(v);adj[v].push_back(u);}int cnt=0;for(int i=1;i<=n;i++)if(!vis[i]){dfs(i);cnt++;}cout<<cnt;}"},
        {"Segment Tree","GNU C++17","Divide and Conquer","Segment Tree,Array","0.15","Time:O(nlogn)","Segment Tree lazy propagation.",
         "#include<bits/stdc++.h>\nusing namespace std;\nlong long t[800005],lz[800005];void push(int v,int tl,int tr){if(lz[v]){t[v]+=lz[v]*(tr-tl+1);if(tl!=tr){lz[2*v]+=lz[v];lz[2*v+1]+=lz[v];}lz[v]=0;}}void upd(int v,int tl,int tr,int l,int r,long long x){push(v,tl,tr);if(l>tr||r<tl)return;if(l<=tl&&tr<=r){lz[v]+=x;push(v,tl,tr);return;}int tm=(tl+tr)/2;upd(2*v,tl,tm,l,r,x);upd(2*v+1,tm+1,tr,l,r,x);t[v]=t[2*v]+t[2*v+1];}int main(){int n,q;cin>>n>>q;while(q--){int l,r;long long v;cin>>l>>r>>v;upd(1,0,n-1,l,r,v);}}"},
        {"Binary Search","GNU C++17","Binary Search,Sorting","Array","0.05","Time:O(nlogn)","Tim kiem nhi phan.",
         "#include<bits/stdc++.h>\nusing namespace std;\nint main(){int n,q;cin>>n>>q;vector<int>a(n);for(int&x:a)cin>>x;sort(a.begin(),a.end());while(q--){int x;cin>>x;cout<<(upper_bound(a.begin(),a.end(),x)-a.begin())<<\"\\n\";}}"},
        {"Union Find","GNU C++17","Greedy","Disjoint Set,Array","0.11","Time:O(n*alpha(n))","Union-Find.",
         "#include<bits/stdc++.h>\nusing namespace std;\nint par[200005],sz[200005];int find(int x){return par[x]==x?x:par[x]=find(par[x]);}void unite(int a,int b){a=find(a);b=find(b);if(a==b)return;if(sz[a]<sz[b])swap(a,b);par[b]=a;sz[a]+=sz[b];}int main(){int n,m;cin>>n>>m;for(int i=1;i<=n;i++){par[i]=i;sz[i]=1;}while(m--){int u,v;cin>>u>>v;unite(u,v);}int cnt=0;for(int i=1;i<=n;i++)if(par[i]==i)cnt++;cout<<cnt;}"}
    };

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        DatabaseManager.getInstance().initialize();
        Connection conn = DatabaseManager.getInstance().getConnection();
        UserDAO userDAO = new UserDAO();
        SubmissionDAO subDAO = new SubmissionDAO();
        AnalysisDAO analysisDAO = new AnalysisDAO();

        System.out.println("=== FIX OLD USERS ===\n");

        String[] targetUsers = {"tourist", "subnet", "benq"};
        Random rng = new Random(123);

        for (String username : targetUsers) {
            User user = userDAO.getUserByUsernameAndPlatform(username, "codeforces");
            if (user == null) {
                System.out.println("[SKIP] " + username + " not found");
                continue;
            }

            int codeCount = subDAO.getSubmissionWithCodeCount(user.getId());
            int analysisCount = analysisDAO.getAnalyzedCount(user.getId());
            System.out.println("[USER] " + username + " id=" + user.getId() + " codeCount=" + codeCount + " analyzed=" + analysisCount);

            if (codeCount >= 5 && analysisCount >= 5) {
                System.out.println("  Already has enough data, skipping.");
                continue;
            }

            // Update existing submissions that lack source code
            List<Submission> subs = subDAO.getSubmissionsByUserId(user.getId());
            int updated = 0;
            for (int i = 0; i < subs.size() && updated < codeSamples.length; i++) {
                Submission s = subs.get(i);
                if (s.getSourceCode() != null && !s.getSourceCode().isEmpty()) continue;

                String[] sample = codeSamples[updated];
                String sql = "UPDATE submissions SET source_code = ?, language = ?, problem_name = ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, sample[7]);
                    ps.setString(2, sample[1]);
                    ps.setString(3, sample[0]);
                    ps.setInt(4, s.getId());
                    ps.executeUpdate();
                }

                // Add analysis if not exists
                if (analysisDAO.getAnalysisForSubmission(s.getId()) == null) {
                    AnalysisResult ar = new AnalysisResult();
                    ar.setSubmissionId(s.getId());
                    ar.setAlgorithms(Arrays.asList(sample[2].split(",")));
                    ar.setDataStructures(Arrays.asList(sample[3].split(",")));
                    ar.setAiProbability(Double.parseDouble(sample[4]));
                    ar.setComplexity(sample[5]);
                    ar.setAiIndicators("Phong cach CP dien hinh.");
                    ar.setAnalysisSummary(sample[6]);
                    analysisDAO.addAnalysisResult(ar);
                }
                updated++;
            }
            System.out.println("  Updated " + updated + " submissions with code + analysis");

            // Generate evaluation
            try {
                AIAnalyzer ai = new AIAnalyzer();
                UserEvaluation eval = ai.evaluateUser(user);
                System.out.println("  Eval: DS=" + String.format("%.0f", eval.getDsScore()) +
                    " Algo=" + String.format("%.0f", eval.getAlgoScore()) +
                    " AI=" + String.format("%.1f%%", eval.getAiUsageRate()));
            } catch (Exception e) {
                System.out.println("  Eval error: " + e.getMessage());
            }
        }

        System.out.println("\n=== DONE ===");
        DatabaseManager.getInstance().close();
        System.exit(0);
    }
}
