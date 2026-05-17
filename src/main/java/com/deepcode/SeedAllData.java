package com.deepcode;

import com.deepcode.dao.*;
import com.deepcode.model.*;
import com.deepcode.service.AIAnalyzer;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class SeedAllData {
    static Connection conn;
    static UserDAO userDAO;
    static SubmissionDAO subDAO;
    static AnalysisDAO analysisDAO;

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        DatabaseManager.getInstance().initialize();
        conn = DatabaseManager.getInstance().getConnection();
        userDAO = new UserDAO();
        subDAO = new SubmissionDAO();
        analysisDAO = new AnalysisDAO();

        System.out.println("=== SEED ALL DATA ===\n");

        // Users: {username, platform}
        String[][] users = {
            {"tourist","codeforces"}, {"subnet","codeforces"},
            {"Benq","codeforces"}, {"jiangly","codeforces"},
            {"Petr","codeforces"}, {"Um_nik","codeforces"},
            {"ecnerwala","codeforces"}, {"mnbvmar","codeforces"}
        };

        for (String[] u : users) {
            User user = userDAO.getUserByUsernameAndPlatform(u[0], u[1]);
            if (user == null) {
                user = new User(u[0], u[1]);
                userDAO.addUser(user);
                System.out.println("[+] User: " + u[0]);
            } else {
                System.out.println("[=] User exists: " + u[0] + " (id=" + user.getId() + ")");
            }
        }

        // Seed submissions + analysis for each user
        List<User> allUsers = userDAO.getAllUsers();
        for (User user : allUsers) {
            seedUserData(user);
        }

        // Generate evaluations
        System.out.println("\n--- Generating evaluations ---");
        AIAnalyzer ai = new AIAnalyzer();
        for (User user : allUsers) {
            try {
                if (analysisDAO.getAnalyzedCount(user.getId()) > 0) {
                    UserEvaluation eval = ai.evaluateUser(user);
                    System.out.println("[OK] " + user.getUsername() + ": DS=" +
                        String.format("%.0f",eval.getDsScore()) + " Algo=" +
                        String.format("%.0f",eval.getAlgoScore()) + " AI=" +
                        String.format("%.1f%%",eval.getAiUsageRate()));
                }
            } catch (Exception e) {
                System.out.println("[SKIP] " + user.getUsername() + ": " + e.getMessage());
            }
        }

        System.out.println("\n=== DONE ===");
        DatabaseManager.getInstance().close();
        System.exit(0);
    }

    // Code samples pool
    static String[][] codeSamples = {
        {"Two Sum","GNU C++17","OK",
         "#include<bits/stdc++.h>\nusing namespace std;\nint main(){int n,t;cin>>n>>t;unordered_map<int,int>mp;vector<int>a(n);for(int i=0;i<n;i++)cin>>a[i];for(int i=0;i<n;i++){if(mp.count(t-a[i])){cout<<mp[t-a[i]]<<\" \"<<i;return 0;}mp[a[i]]=i;}cout<<-1;}",
         "Two Pointers,Greedy", "Array,HashMap", "0.10", "Time:O(n),Space:O(n)", "Tim 2 so co tong bang target bang HashMap."},
        {"Dijkstra Shortest Path","GNU C++17","OK",
         "#include<bits/stdc++.h>\nusing namespace std;\ntypedef pair<int,int>pii;int main(){int n,m,s,t;cin>>n>>m>>s>>t;vector<vector<pii>>g(n+1);for(int i=0;i<m;i++){int u,v,w;cin>>u>>v>>w;g[u].push_back({v,w});g[v].push_back({u,w});}vector<int>d(n+1,1e9);priority_queue<pii,vector<pii>,greater<pii>>pq;d[s]=0;pq.push({0,s});while(!pq.empty()){auto[c,u]=pq.top();pq.pop();if(c>d[u])continue;for(auto[v,w]:g[u])if(d[u]+w<d[v]){d[v]=d[u]+w;pq.push({d[v],v});}}cout<<d[t];}",
         "Dijkstra,Greedy", "Graph,Priority Queue,Array", "0.08", "Time:O((V+E)logV),Space:O(V+E)", "Dijkstra tim duong di ngan nhat."},
        {"Knapsack DP","GNU C++17","OK",
         "#include<bits/stdc++.h>\nusing namespace std;\nint main(){int n,W;cin>>n>>W;vector<int>w(n),v(n);for(int i=0;i<n;i++)cin>>w[i]>>v[i];vector<vector<int>>dp(n+1,vector<int>(W+1,0));for(int i=1;i<=n;i++)for(int j=0;j<=W;j++){dp[i][j]=dp[i-1][j];if(j>=w[i-1])dp[i][j]=max(dp[i][j],dp[i-1][j-w[i-1]]+v[i-1]);}cout<<dp[n][W];}",
         "Dynamic Programming", "Array,2D Array", "0.12", "Time:O(nW),Space:O(nW)", "Bai toan ba lo bang quy hoach dong."},
        {"Binary Search","GNU C++17","OK",
         "#include<bits/stdc++.h>\nusing namespace std;\nint main(){int n,q;cin>>n>>q;vector<int>a(n);for(int&x:a)cin>>x;sort(a.begin(),a.end());while(q--){int x;cin>>x;cout<<(upper_bound(a.begin(),a.end(),x)-a.begin())<<\"\\n\";}}",
         "Binary Search,Sorting", "Array", "0.05", "Time:O((n+q)logn),Space:O(n)", "Tim kiem nhi phan tren mang da sap xep."},
        {"Segment Tree","GNU C++17","OK",
         "#include<bits/stdc++.h>\nusing namespace std;\nconst int N=2e5+5;long long t[4*N],lz[4*N];void push(int v,int tl,int tr){if(lz[v]){t[v]+=lz[v]*(tr-tl+1);if(tl!=tr){lz[2*v]+=lz[v];lz[2*v+1]+=lz[v];}lz[v]=0;}}void upd(int v,int tl,int tr,int l,int r,long long x){push(v,tl,tr);if(l>tr||r<tl)return;if(l<=tl&&tr<=r){lz[v]+=x;push(v,tl,tr);return;}int tm=(tl+tr)/2;upd(2*v,tl,tm,l,r,x);upd(2*v+1,tm+1,tr,l,r,x);t[v]=t[2*v]+t[2*v+1];}long long qry(int v,int tl,int tr,int l,int r){push(v,tl,tr);if(l>tr||r<tl)return 0;if(l<=tl&&tr<=r)return t[v];int tm=(tl+tr)/2;return qry(2*v,tl,tm,l,r)+qry(2*v+1,tm+1,tr,l,r);}int main(){int n,q;cin>>n>>q;for(int i=0;i<n;i++){int x;cin>>x;upd(1,0,n-1,i,i,x);}while(q--){int tp;cin>>tp;if(tp==1){int l,r;long long v;cin>>l>>r>>v;upd(1,0,n-1,l-1,r-1,v);}else{int l,r;cin>>l>>r;cout<<qry(1,0,n-1,l-1,r-1)<<\"\\n\";}}}",
         "Divide and Conquer,Dynamic Programming", "Segment Tree,Array", "0.15", "Time:O(nlogn),Space:O(4n)", "Segment Tree voi lazy propagation."},
        {"BFS Grid","GNU C++17","OK",
         "#include<bits/stdc++.h>\nusing namespace std;\nint dx[]={0,0,1,-1},dy[]={1,-1,0,0};int main(){int n,m;cin>>n>>m;vector<string>g(n);for(auto&s:g)cin>>s;int sx,sy,ex,ey;for(int i=0;i<n;i++)for(int j=0;j<m;j++){if(g[i][j]=='S'){sx=i;sy=j;}if(g[i][j]=='E'){ex=i;ey=j;}}vector<vector<int>>d(n,vector<int>(m,-1));queue<pair<int,int>>q;q.push({sx,sy});d[sx][sy]=0;while(!q.empty()){auto[x,y]=q.front();q.pop();for(int i=0;i<4;i++){int nx=x+dx[i],ny=y+dy[i];if(nx>=0&&nx<n&&ny>=0&&ny<m&&g[nx][ny]!='#'&&d[nx][ny]==-1){d[nx][ny]=d[x][y]+1;q.push({nx,ny});}}}cout<<d[ex][ey];}",
         "BFS", "Graph,Queue,2D Array", "0.07", "Time:O(NM),Space:O(NM)", "BFS tim duong ngan nhat tren luoi."},
        {"DFS Components","GNU C++17","OK",
         "#include<bits/stdc++.h>\nusing namespace std;\nvector<int>adj[200005];bool vis[200005];void dfs(int u){vis[u]=1;for(int v:adj[u])if(!vis[v])dfs(v);}int main(){int n,m;cin>>n>>m;for(int i=0;i<m;i++){int u,v;cin>>u>>v;adj[u].push_back(v);adj[v].push_back(u);}int cnt=0;for(int i=1;i<=n;i++)if(!vis[i]){dfs(i);cnt++;}cout<<cnt;}",
         "DFS", "Graph,Array", "0.06", "Time:O(V+E),Space:O(V+E)", "DFS dem so thanh phan lien thong."},
        {"LIS DP","GNU C++17","OK",
         "#include<bits/stdc++.h>\nusing namespace std;\nint main(){int n;cin>>n;vector<int>a(n),dp;for(int&x:a)cin>>x;for(int x:a){auto it=lower_bound(dp.begin(),dp.end(),x);if(it==dp.end())dp.push_back(x);else *it=x;}cout<<dp.size();}",
         "Dynamic Programming,Binary Search", "Array", "0.09", "Time:O(nlogn),Space:O(n)", "Tim day con tang dai nhat (LIS) bang DP + Binary Search."},
        {"Union Find","GNU C++17","OK",
         "#include<bits/stdc++.h>\nusing namespace std;\nint par[200005],sz[200005];int find(int x){return par[x]==x?x:par[x]=find(par[x]);}void unite(int a,int b){a=find(a);b=find(b);if(a==b)return;if(sz[a]<sz[b])swap(a,b);par[b]=a;sz[a]+=sz[b];}int main(){int n,m;cin>>n>>m;for(int i=1;i<=n;i++){par[i]=i;sz[i]=1;}while(m--){int u,v;cin>>u>>v;unite(u,v);}int cnt=0;for(int i=1;i<=n;i++)if(par[i]==i)cnt++;cout<<cnt;}",
         "Greedy", "Disjoint Set,Array", "0.11", "Time:O(n*alpha(n)),Space:O(n)", "Union-Find dem so thanh phan lien thong."},
        {"Topological Sort","GNU C++17","OK",
         "#include<bits/stdc++.h>\nusing namespace std;\nint main(){int n,m;cin>>n>>m;vector<vector<int>>g(n+1);vector<int>in(n+1,0);for(int i=0;i<m;i++){int u,v;cin>>u>>v;g[u].push_back(v);in[v]++;}queue<int>q;for(int i=1;i<=n;i++)if(!in[i])q.push(i);vector<int>res;while(!q.empty()){int u=q.front();q.pop();res.push_back(u);for(int v:g[u])if(--in[v]==0)q.push(v);}if((int)res.size()!=n)cout<<\"IMPOSSIBLE\";else for(int x:res)cout<<x<<\" \";}",
         "Topological Sort,BFS", "Graph,Queue,Array", "0.10", "Time:O(V+E),Space:O(V+E)", "Sap xep topo bang BFS (thuat toan Kahn)."},
        {"String Hashing","GNU C++17","WRONG_ANSWER",
         "#include<bits/stdc++.h>\nusing namespace std;\nconst long long MOD=1e9+7,BASE=31;int main(){string s;cin>>s;int n=s.size();vector<long long>h(n+1,0),pw(n+1,1);for(int i=0;i<n;i++){h[i+1]=(h[i]*BASE+s[i]-'a'+1)%MOD;pw[i+1]=pw[i]*BASE%MOD;}int q;cin>>q;while(q--){int l1,r1,l2,r2;cin>>l1>>r1>>l2>>r2;auto get=[&](int l,int r){return(h[r+1]-h[l]*pw[r-l+1]%MOD+MOD*MOD)%MOD;};cout<<(get(l1,r1)==get(l2,r2)?\"Yes\":\"No\")<<\"\\n\";}}",
         "Math,Greedy", "Array,String", "0.13", "Time:O(n+q),Space:O(n)", "So sanh chuoi con bang hash."},
        {"Floyd Warshall","GNU C++17","OK",
         "#include<bits/stdc++.h>\nusing namespace std;\nconst int INF=1e9;int d[505][505];int main(){int n,m;cin>>n>>m;for(int i=1;i<=n;i++)for(int j=1;j<=n;j++)d[i][j]=(i==j?0:INF);for(int i=0;i<m;i++){int u,v,w;cin>>u>>v>>w;d[u][v]=min(d[u][v],w);}for(int k=1;k<=n;k++)for(int i=1;i<=n;i++)for(int j=1;j<=n;j++)d[i][j]=min(d[i][j],d[i][k]+d[k][j]);int q;cin>>q;while(q--){int u,v;cin>>u>>v;cout<<(d[u][v]>=INF?-1:d[u][v])<<\"\\n\";}}",
         "Floyd-Warshall,Dynamic Programming", "2D Array,Graph", "0.14", "Time:O(n^3),Space:O(n^2)", "Floyd-Warshall: duong di ngan nhat moi cap dinh."}
    };

    static Random rng = new Random(42);

    static void seedUserData(User user) throws Exception {
        int existing = subDAO.getSubmissionCount(user.getId());
        int analysisCount = analysisDAO.getAnalyzedCount(user.getId());
        
        if (existing >= 8 && analysisCount >= 5) {
            System.out.println("[=] " + user.getUsername() + ": already has " + existing + " subs, " + analysisCount + " analyzed");
            return;
        }

        System.out.println("[+] Seeding " + user.getUsername() + "...");

        // Pick 8 random code samples for this user
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < codeSamples.length; i++) indices.add(i);
        Collections.shuffle(indices, rng);

        int toAdd = Math.max(0, 8 - existing);
        int added = 0;

        for (int k = 0; k < Math.min(toAdd, indices.size()); k++) {
            int idx = indices.get(k);
            String[] s = codeSamples[idx];
            String subId = "seed-" + user.getId() + "-" + System.nanoTime();
            
            if (subDAO.submissionExists(user.getId(), subId)) continue;

            Submission sub = new Submission();
            sub.setUserId(user.getId());
            sub.setSubmissionId(subId);
            sub.setProblemName(s[0]);
            sub.setProblemId("CF" + (1000 + idx));
            sub.setLanguage(s[1]);
            sub.setVerdict(s[2]);
            sub.setSourceCode(s[3]);
            sub.setSubmissionTime(LocalDateTime.now().minusDays(rng.nextInt(30)));
            int id = subDAO.addSubmission(sub);
            
            if (id > 0) {
                // Insert analysis result
                AnalysisResult ar = new AnalysisResult();
                ar.setSubmissionId(id);
                ar.setAlgorithms(Arrays.asList(s[4].split(",")));
                ar.setDataStructures(Arrays.asList(s[5].split(",")));
                ar.setAiProbability(Double.parseDouble(s[6]));
                ar.setComplexity(s[7]);
                ar.setAiIndicators("Phong cach CP dien hinh, bien ngan gon, khong comment thua.");
                ar.setAnalysisSummary(s[8]);
                analysisDAO.addAnalysisResult(ar);
                added++;
            }
        }
        
        System.out.println("    Added " + added + " submissions + analysis");
    }
}
