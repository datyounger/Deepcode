package com.deepcode;

import com.deepcode.dao.AnalysisDAO;
import com.deepcode.dao.DatabaseManager;
import com.deepcode.dao.SubmissionDAO;
import com.deepcode.dao.UserDAO;
import com.deepcode.model.AnalysisResult;
import com.deepcode.model.Submission;
import com.deepcode.model.User;
import com.deepcode.model.UserEvaluation;
import com.deepcode.service.AIAnalyzer;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Utility to seed sample data, analysis results, and generate evaluation reports.
 * Can work in two modes:
 *   - Online mode: uses Gemini AI to analyze code (requires valid API key with quota)
 *   - Offline mode: inserts pre-computed analysis results for demonstration
 */
public class AnalyzeAndReport {

    // Sample source codes for competitive programming problems
    private static final String[][] SAMPLE_CODES = {
        {"Two Sum", "GNU C++17",
            "#include <bits/stdc++.h>\nusing namespace std;\nint main(){\n    int n, target;\n    cin >> n >> target;\n    unordered_map<int,int> mp;\n    vector<int> a(n);\n    for(int i=0;i<n;i++) cin >> a[i];\n    for(int i=0;i<n;i++){\n        if(mp.count(target - a[i])){\n            cout << mp[target-a[i]] << \" \" << i << endl;\n            return 0;\n        }\n        mp[a[i]] = i;\n    }\n    cout << -1 << endl;\n}"},
        {"Shortest Path (Dijkstra)", "GNU C++17",
            "#include <bits/stdc++.h>\nusing namespace std;\ntypedef pair<int,int> pii;\nconst int INF = 1e9;\nint main(){\n    int n,m,s,t;\n    cin>>n>>m>>s>>t;\n    vector<vector<pii>> adj(n+1);\n    for(int i=0;i<m;i++){\n        int u,v,w; cin>>u>>v>>w;\n        adj[u].push_back({v,w});\n        adj[v].push_back({u,w});\n    }\n    vector<int> dist(n+1, INF);\n    priority_queue<pii, vector<pii>, greater<pii>> pq;\n    dist[s]=0; pq.push({0,s});\n    while(!pq.empty()){\n        auto [d,u] = pq.top(); pq.pop();\n        if(d > dist[u]) continue;\n        for(auto [v,w] : adj[u]){\n            if(dist[u]+w < dist[v]){\n                dist[v] = dist[u]+w;\n                pq.push({dist[v], v});\n            }\n        }\n    }\n    cout << (dist[t]==INF ? -1 : dist[t]) << endl;\n}"},
        {"Knapsack DP", "GNU C++17",
            "#include <bits/stdc++.h>\nusing namespace std;\nint main(){\n    int n, W;\n    cin >> n >> W;\n    vector<int> w(n), v(n);\n    for(int i=0;i<n;i++) cin >> w[i] >> v[i];\n    vector<vector<int>> dp(n+1, vector<int>(W+1, 0));\n    for(int i=1;i<=n;i++){\n        for(int j=0;j<=W;j++){\n            dp[i][j] = dp[i-1][j];\n            if(j >= w[i-1])\n                dp[i][j] = max(dp[i][j], dp[i-1][j-w[i-1]] + v[i-1]);\n        }\n    }\n    cout << dp[n][W] << endl;\n}"},
        {"Binary Search", "GNU C++17",
            "#include <bits/stdc++.h>\nusing namespace std;\nint main(){\n    int n, q;\n    cin >> n >> q;\n    vector<int> a(n);\n    for(int i=0;i<n;i++) cin >> a[i];\n    sort(a.begin(), a.end());\n    while(q--){\n        int x; cin >> x;\n        int lo=0, hi=n-1, ans=-1;\n        while(lo<=hi){\n            int mid=(lo+hi)/2;\n            if(a[mid]<=x){ ans=mid; lo=mid+1; }\n            else hi=mid-1;\n        }\n        cout << ans+1 << \"\\n\";\n    }\n}"},
        {"Segment Tree Range Update", "GNU C++17",
            "#include <bits/stdc++.h>\nusing namespace std;\nconst int MAXN = 2e5+5;\nlong long tree[4*MAXN], lazy[4*MAXN];\nvoid build(int node, int s, int e, vector<int>& a){\n    if(s==e){ tree[node]=a[s]; return; }\n    int mid=(s+e)/2;\n    build(2*node, s, mid, a);\n    build(2*node+1, mid+1, e, a);\n    tree[node] = tree[2*node] + tree[2*node+1];\n}\nvoid push(int node, int s, int e){\n    if(lazy[node]){\n        tree[node] += lazy[node]*(e-s+1);\n        if(s!=e){ lazy[2*node]+=lazy[node]; lazy[2*node+1]+=lazy[node]; }\n        lazy[node]=0;\n    }\n}\nvoid update(int node, int s, int e, int l, int r, long long val){\n    push(node,s,e);\n    if(s>r||e<l) return;\n    if(l<=s&&e<=r){ lazy[node]+=val; push(node,s,e); return; }\n    int mid=(s+e)/2;\n    update(2*node,s,mid,l,r,val);\n    update(2*node+1,mid+1,e,l,r,val);\n    tree[node]=tree[2*node]+tree[2*node+1];\n}\nlong long query(int node, int s, int e, int l, int r){\n    push(node,s,e);\n    if(s>r||e<l) return 0;\n    if(l<=s&&e<=r) return tree[node];\n    int mid=(s+e)/2;\n    return query(2*node,s,mid,l,r)+query(2*node+1,mid+1,e,l,r);\n}\nint main(){\n    int n,q; cin>>n>>q;\n    vector<int> a(n);\n    for(int i=0;i<n;i++) cin>>a[i];\n    build(1,0,n-1,a);\n    while(q--){\n        int type; cin>>type;\n        if(type==1){ int l,r; long long v; cin>>l>>r>>v; update(1,0,n-1,l-1,r-1,v); }\n        else{ int l,r; cin>>l>>r; cout<<query(1,0,n-1,l-1,r-1)<<\"\\n\"; }\n    }\n}"}
    };

    // Pre-computed analysis results (simulates Gemini AI output)
    private static final Object[][] ANALYSIS_DATA = {
        // {algorithms[], dataStructures[], aiProbability, aiIndicators, complexity, summary}
        {
            new String[]{"Two Pointers", "Greedy"},
            new String[]{"Array", "HashMap"},
            0.10,
            "Code su dung phong cach CP dien hinh: macro ngan gon, khong comment. Xac suat AI thap.",
            "Time: O(n), Space: O(n)",
            "Bai toan Two Sum: Tim 2 phan tu trong mang co tong bang target. Su dung HashMap de luu index, duyet 1 lan O(n)."
        },
        {
            new String[]{"Dijkstra", "Greedy"},
            new String[]{"Graph", "Priority Queue", "Array"},
            0.08,
            "Su dung structured binding (auto [d,u]), phong cach CP ngan gon. Xac suat AI rat thap.",
            "Time: O((V+E) log V), Space: O(V+E)",
            "Thuat toan Dijkstra tim duong di ngan nhat tu dinh s den t. Su dung priority queue toi uu hoa."
        },
        {
            new String[]{"Dynamic Programming"},
            new String[]{"Array", "2D Array"},
            0.12,
            "Code DP co ban, phong cach viet gon, bien ngan. Dac trung cua nguoi lam CP.",
            "Time: O(n*W), Space: O(n*W)",
            "Bai toan Knapsack (ba lo): Quy hoach dong 2 chieu de tim gia tri lon nhat khi chon do vat voi gioi han trong luong."
        },
        {
            new String[]{"Binary Search", "Sorting"},
            new String[]{"Array"},
            0.05,
            "Code rat ngan gon, su dung bien lo/hi/mid/ans dien hinh CP. Xac suat AI cuc thap.",
            "Time: O((n+q) log n), Space: O(n)",
            "Tim kiem nhi phan tren mang da sap xep. Voi moi truy van, tim vi tri lon nhat co gia tri <= x."
        },
        {
            new String[]{"Divide and Conquer", "Dynamic Programming"},
            new String[]{"Segment Tree", "Array"},
            0.15,
            "Segment Tree voi lazy propagation - ky thuat nang cao. Code gon, khong comment, dien hinh CP.",
            "Time: O(n + q*log n), Space: O(4*n)",
            "Segment Tree voi range update va range query. Su dung lazy propagation de cap nhat doan trong O(log n)."
        }
    };

    public static void main(String[] args) {
        try {
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

            System.out.println("===========================================");
            System.out.println("  DeepCode - Phan tich & Bao cao tu dong");
            System.out.println("===========================================\n");

            DatabaseManager.getInstance().initialize();
            UserDAO userDAO = new UserDAO();
            SubmissionDAO submissionDAO = new SubmissionDAO();
            AnalysisDAO analysisDAO = new AnalysisDAO();
            AIAnalyzer aiAnalyzer = new AIAnalyzer();

            List<User> users = userDAO.getAllUsers();
            if (users.isEmpty()) {
                System.out.println("[INFO] Khong co user nao. Dang tao user mau...");
                User user = new User("tourist", "codeforces");
                int id = userDAO.addUser(user);
                user.setId(id);
                users = userDAO.getAllUsers();
            }

            User user = users.get(0);
            System.out.println("[USER] Xu ly: " + user.getUsername() + " (" + user.getPlatform() + ")\n");

            // === STEP 1: Ensure source code exists ===
            int codeCount = submissionDAO.getSubmissionWithCodeCount(user.getId());
            System.out.println("[INFO] Submissions co source code: " + codeCount);

            if (codeCount == 0) {
                System.out.println("[STEP 1] Chen ma nguon mau vao database...");
                Connection conn = DatabaseManager.getInstance().getConnection();
                List<Submission> subs = submissionDAO.getSubmissionsByUserId(user.getId());

                int inserted = 0;
                if (subs.size() >= SAMPLE_CODES.length) {
                    for (int i = 0; i < SAMPLE_CODES.length; i++) {
                        Submission s = subs.get(i);
                        String sql = "UPDATE submissions SET source_code = ?, language = ?, problem_name = ? WHERE id = ?";
                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, SAMPLE_CODES[i][2]);
                            ps.setString(2, SAMPLE_CODES[i][1]);
                            ps.setString(3, SAMPLE_CODES[i][0]);
                            ps.setInt(4, s.getId());
                            ps.executeUpdate();
                            inserted++;
                            System.out.println("  [OK] " + SAMPLE_CODES[i][0] + " -> Submission #" + s.getId());
                        }
                    }
                } else {
                    for (String[] sample : SAMPLE_CODES) {
                        Submission s = new Submission();
                        s.setUserId(user.getId());
                        s.setSubmissionId("sample-" + System.nanoTime());
                        s.setProblemName(sample[0]);
                        s.setProblemId("SAMPLE");
                        s.setLanguage(sample[1]);
                        s.setVerdict("OK");
                        s.setSourceCode(sample[2]);
                        s.setSubmissionTime(LocalDateTime.now());
                        submissionDAO.addSubmission(s);
                        inserted++;
                        System.out.println("  [OK] Tao moi: " + sample[0]);
                    }
                }
                System.out.println("[DONE] Da chen " + inserted + " ma nguon.\n");
            }

            // === STEP 2: Try Gemini AI first, fallback to offline data ===
            boolean useOffline = false;
            int analyzedCount = analysisDAO.getAnalyzedCount(user.getId());

            if (analyzedCount == 0) {
                System.out.println("[STEP 2] Thu phan tich bang Gemini AI...");
                try {
                    int result = aiAnalyzer.analyzeUserSubmissions(user, msg -> System.out.println("  " + msg));
                    if (result == 0) {
                        System.out.println("  [WARN] Gemini API khong tra ve ket qua. Chuyen sang che do offline.");
                        useOffline = true;
                    }
                } catch (Exception e) {
                    System.out.println("  [WARN] Gemini API loi: " + e.getMessage());
                    System.out.println("  [INFO] Chuyen sang che do offline (du lieu mau).\n");
                    useOffline = true;
                }
            }

            // Insert offline analysis data if needed
            analyzedCount = analysisDAO.getAnalyzedCount(user.getId());
            if (analyzedCount == 0 || useOffline) {
                System.out.println("[STEP 2b] Chen ket qua phan tich mau (offline mode)...");
                List<Submission> subsWithCode = submissionDAO.getSubmissionsByUserId(user.getId());

                int idx = 0;
                for (Submission s : subsWithCode) {
                    if (s.getSourceCode() == null || s.getSourceCode().isEmpty()) continue;
                    if (idx >= ANALYSIS_DATA.length) break;

                    // Check if already analyzed
                    if (analysisDAO.getAnalysisForSubmission(s.getId()) != null) {
                        idx++;
                        continue;
                    }

                    Object[] data = ANALYSIS_DATA[idx];
                    AnalysisResult ar = new AnalysisResult();
                    ar.setSubmissionId(s.getId());
                    ar.setAlgorithms(Arrays.asList((String[]) data[0]));
                    ar.setDataStructures(Arrays.asList((String[]) data[1]));
                    ar.setAiProbability((double) data[2]);
                    ar.setAiIndicators((String) data[3]);
                    ar.setComplexity((String) data[4]);
                    ar.setAnalysisSummary((String) data[5]);

                    analysisDAO.addAnalysisResult(ar);
                    System.out.println("  [OK] " + s.getProblemName() + " -> AI Prob: " + (int)((double)data[2]*100) + "%");
                    idx++;
                }
                System.out.println("[DONE] Da chen " + idx + " ket qua phan tich.\n");
            }

            // === STEP 3: Generate evaluation report ===
            System.out.println("[STEP 3] Tao bao cao danh gia tong the...\n");
            try {
                UserEvaluation eval = aiAnalyzer.evaluateUser(user);

                System.out.println("===========================================");
                System.out.println("  KET QUA BAO CAO - " + user.getUsername());
                System.out.println("===========================================\n");
                System.out.println(eval.getEvaluationSummary());
                System.out.println("-------------------------------------------");
                System.out.println("  Diem CTDL:       " + String.format("%.1f", eval.getDsScore()) + "/100 " + eval.getDsLevel());
                System.out.println("  Diem Thuat toan: " + String.format("%.1f", eval.getAlgoScore()) + "/100 " + eval.getAlgoLevel());
                System.out.println("  Muc su dung AI:  " + String.format("%.1f", eval.getAiUsageRate()) + "% " + eval.getAiUsageLevel());
                System.out.println("-------------------------------------------");
                System.out.println("\n===========================================");
                System.out.println("  HOAN TAT THANH CONG!");
                System.out.println("===========================================");
            } catch (Exception e) {
                System.out.println("[ERROR] Khong the tao bao cao: " + e.getMessage());
            }

            DatabaseManager.getInstance().close();
            System.exit(0);

        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
