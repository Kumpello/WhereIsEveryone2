# Perfetto analysis: normal app interactions

Analyzed `/home/kumpel/Downloads/android-202693-104833.pftrace`, confirmed as the file imported into the current Android Studio profiler session.

Evidence tags: **[SQL]** means measured in this capture; **[INFERRED]** means an explanation or proposed action supported by the evidence; **[GAP]** means the recording cannot establish the claim. Times are relative to the trace start unless a clock time is specified.

## Capture and scope

- [SQL] Recorded **2026-10-14 10:48:33.581–10:50:13.705 CEST**, **100.123 seconds**, on the Samsung S26/S942B platform, Android 16/API 36.
- User-described flow: login, several map swipes, status entry, attempted friend addition, then Settings and proximity-alert changes.
- [SQL] App process `com.kumpello.whereiseveryone`: UPID **1017**, PID **24728**. Main thread: UTID **1106**. Main HWUI RenderThread: UTID **9514**. Principal Mapbox renderer: UTID **10803**, `MbxRender569061`.
- [SQL] Trace bounds: **2982387674480863–2982487797690392 ns**. Analysis used Perfetto trace processor **v57.2**.
- [GAP] The capture predates the **11:10 location-service task-removal commit**, so it does not validate the swipe-away shutdown change. There is no recorded swipe-away scenario in the described flow.
- [GAP] The trace contains no app CPU call-stack samples or allocation/heap dump. One `systrace_parse_failure` was reported; no positive loss/drop statistic was identified. Several sleep states lack waker/blocked-function metadata. The current R8 mapping was generated at **11:07**, after this capture, and cannot independently identify the recorded APK.

## Findings, in priority order

### 1. Recreating the map produces visible UI stalls

[SQL] The initial map frame starts at **+29.609168 s** and the main-thread `doFrame` lasts **71.471 ms**, compared with **71.054 ms** in the corresponding submitted-frame timeline. Another map creation at **+95.315965 s** produces a **42.072 ms** main-thread frame and a **47.368 ms** submitted-frame duration.

[SQL] The first frame consists of **30.616 ms CPU**, **39.225 ms sleeping**, **0.828 ms uninterruptible wait**, and **0.803 ms runnable delay**. Composition occupies **19.415 ms**. Main sleeps for **23.289 ms** at **+29.643286 s**, while Mapbox's Vulkan instance/device/swapchain initialization runs at **+29.645817–29.662107 s**. Main then waits on RenderThread for **9.695 ms** at **+29.670603 s**.

[SQL] On the later creation, main sleeps **28.304 ms** at **+95.326994 s**, overlapping Mapbox Vulkan initialization at **+95.330233–95.350778 s**. Runnable delay is only **1.405 ms** across that frame.

[INFERRED] Synchronous map/graphics initialization is the strongest explanation for these creation hitches. Repeated construction makes this cost recur when returning to the map. [GAP] Missing waker/futex metadata prevents identifying the exact native synchronization object for the Mapbox wait; overlap alone does not prove every millisecond of sleeping was caused by Vulkan.

[SQL] Native renderers live at **+29.619–71.350 s**, **+81.383–83.878 s**, and **+95.298 s–trace end**, with no overlap. Old renderer threads stop after navigation; this is repeated initialization, with no observed accumulating renderer leak.

[INFERRED] First optimization to evaluate: reduce repeated map/graphics construction during navigation while preserving lifecycle cleanup. This trades some retained resources for smoother returns and must be measured rather than assumed to improve memory use.

### 2. A graphics-pipeline cache miss causes a separate interaction hitch

[SQL] At **+92.156198 s**, main's `doFrame` is only **2.411 ms**, while HWUI RenderThread's `DrawFrames` lasts **39.424 ms** with **36.383 ms CPU**. Inside it, `FillRectOp` lasts **35.142 ms**, including **23.772 ms** in `CreateGraphicsPipeline-CompileAfterCacheMiss` at **+92.172817 s**, with **20.798 ms CPU**.

[SQL] The next main frame at **+92.164502 s** blocks in explicit `postAndWait` for **31.728 ms**, until RenderThread finishes at **+92.197666 s**. This establishes the dependency chain: **main → RenderThread → FillRectOp → Vulkan graphics-pipeline compilation**. RenderThread also has **2.489 ms** of uninterruptible wait; CPU compilation is the dominant cost.

[SQL] Motion MOVE events surround this window, and there is only one recorded `CompileAfterCacheMiss` event in the app. [INFERRED] This is consistent with a drag interaction late in the Settings portion of the user's flow. [GAP] The trace does not identify the particular slider/ripple/shape or shader, so it cannot establish a defect in proximity calculation or its UI layout.

[INFERRED] Treat this as a one-off graphics compilation hitch to check on repeated, warmed interactions before changing Settings code.

### 3. Continuous high-rate map/camera animation is a resource-cost candidate

[SQL] The first Mapbox renderer emits **4,802 animation frames** at approximately **118–120 fps** across the visible map portion. Animation slices consume **18.359 CPU seconds**, out of that renderer's **21.753 CPU seconds**. Typical animation wall times are **3.817 ms median**, **5.188 ms p95**, and **6.264 ms p99**.

[SQL] Between app touch events at **+54.505–66.541 s**, the map still emits **1,428 frames** and consumes **5.445 CPU seconds** in animation slices. Keyboard `InputConnection` work continues during this interval. [GAP] This is not a proven idle-map bug: heading, location and camera coordinates were not recorded as independently queryable values.

[SQL] Main records **4,427 AnimatorSet end events** and approximately **4,196–4,197 ends per camera-axis label** in **+30–70 s**. [INFERRED] The current mapping associates those labels with Mapbox bearing/center/padding/pitch/zoom animators, suggesting frequent follow-puck/camera updates. [GAP] The mapping is newer than the capture, so the exact class correspondence remains provisional.

[INFERRED] Inspect redundant camera targets and animation restarts first. A lower frame limit is another measurable tradeoff if desired; Mapbox exposes [setMaximumFps](https://docs.mapbox.com/android/maps/api/latest/mapbox-maps-android/com.mapbox.maps/-map-view/). This capture does not justify changing map or marker behavior automatically.

## Severity and confounds

- [SQL] **3,234 distinct submitted app-surface frame tokens**, **107 app-deadline misses (3.309%)**, and **39 dropped tokens**. Authentication: **785 frames / 33 misses**; MainActivity: **2,449 frames / 74 misses**. **12** missed-deadline frames exceed **16.67 ms**, **7** exceed **25 ms**, and **4** exceed **33 ms**.
- [GAP] These are submitted app-surface frames, not a denominator for the user's perceived map/video frame rate. Surface/TX duplicates were removed. Buffer-stuffing and SurfaceFlinger flags must not all be counted as app-caused stutters; [Perfetto's FrameTimeline documentation](https://perfetto.dev/docs/data-sources/frametimeline) distinguishes latency from missed app deadlines.
- [SQL] App CPU total: **64.329 CPU seconds** over the capture, about **0.643 busy core on average**. Main uses **13.874 CPU seconds**, HWUI **7.346**. The phone's busiest one-second interval uses **5.355 of 10 cores**; no whole-device CPU saturation is established.
- [SQL] CPU-temperature counters peak at **48°C**, GPU at **40°C**. During map activity, GPU frequency is **226–461 MHz**, below a constant **980 MHz** maximum-limit counter; CPU clusters still reach **2.764/3.264/3.801 GHz**. [GAP] No explicit cooling/throttle track was recorded, so this is absence of positive throttling evidence rather than proof none occurred.
- [SQL] App RSS ranges **157.625–345.996 MiB**, ends at **337.496 MiB**, and repeatedly declines between actions. GPU-memory accounting peaks at **300.188 MiB**. [GAP] Do not add GPU memory and RSS blindly; accounting may overlap. No leak can be established without retained-object/allocation evidence.
- [SQL] `system_server` LowMemThread UTID **1654** records four `criticalLowMemory` slices at **+30.389/+31.489/+32.573/+33.675 s**, lasting **664.677/652.601/871.986/858.513 ms**, with no recorded LMK events. [GAP] These are system-pressure callbacks during map loading; their causal contribution to app jank is unproven and they do not overlap the initial +29.609 s hitch.
- [SQL] Six concurrent GCs total **173.443 ms**, but recorded suspend-all pauses total only **0.141 ms**, maximum **0.0795 ms**. No significant stop-the-world GC stall is established.
- [SQL] Captured logs contain **zero app FATAL exceptions and zero matching app ANR reports**. [GAP] This is limited to the recorded session.

## Other captured warnings

- [SQL] Login is rejected with **HTTP 403 at +5.214 s**; friend addition is rejected with **HTTP 404 at +76.394 s**. [GAP] The trace lacks the rejected inputs/server response bodies, so these may be expected incorrect-login or nonexistent-user responses.
- [SQL] Every map creation logs temporary missing sources for the friends/user-marker layers, missing `mapbox-location-shadow-icon`, and follow-puck initialization before the location component is enabled. [INFERRED] These are initialization/configuration cleanup candidates; the capture does not show that they caused an ongoing visible-marker failure.
- [SQL] First map initialization also logs Mapbox class-lookup failures. [GAP] This capture does not distinguish benign class-loader probes from an R8/JNI integration problem. No runtime crash follows in the recording.
- [SQL] Telemetry flush warnings explicitly say events dispatch is disabled. [INFERRED] They are consistent with intentional telemetry disabling rather than a failed app backend upload.

## Reproducing the analysis

Use a trace-processor session to parse the capture once, then execute the SQL blocks below. Each table schema was checked with `LIMIT 0` during analysis. IDs and timestamp windows are specific to this trace and processor version. The seven Android v1 metrics require about **74 seconds** once; do not repeatedly recalculate them.

```sh
python3 data/skills/android-profiler/references/perfetto/bin/trace_processor server unix \
  --name whereiseveryone-profile \
  /home/kumpel/Downloads/android-202693-104833.pftrace
# In another terminal:
python3 data/skills/android-profiler/references/perfetto/bin/trace_processor query \
  --remote whereiseveryone-profile 'SELECT * FROM trace_bounds;'
```

The remaining queries reproduce the specific findings, counters, and dependency windows. No application/backend source or device state was changed for this review.

## System, CPU, memory and thermal queries

```sql
-- Metadata, clock calibration and integrity.
SELECT m.name, m.int_value, m.str_value
FROM metadata m
WHERE m.name IN ('android_build_fingerprint', 'android_sdk_version',
  'android_soc_model', 'ftrace_setup_errors', 'trace_uuid');
SELECT b.start_ts, b.end_ts, (b.end_ts-b.start_ts)/1e9 AS duration_s
FROM trace_bounds b;
SELECT c.clock_name, min(c.ts) AS trace_ts,
  min(c.clock_value) AS clock_value
FROM clock_snapshot c GROUP BY c.clock_name;
SELECT s.name, s.severity, s.idx, s.value
FROM stats s
WHERE s.value > 0 AND
  (s.severity != 'info' OR s.name GLOB '*loss*' OR s.name GLOB '*drop*');
```

```sql
-- Skill-required metrics; already computed.
SELECT RUN_METRIC('android/android_cpu.sql');
SELECT RUN_METRIC('android/android_mem.sql');
SELECT RUN_METRIC('android/android_lmk.sql');
SELECT RUN_METRIC('android/android_gpu.sql');
SELECT RUN_METRIC('android/android_binder.sql');
SELECT RUN_METRIC('android/android_surfaceflinger.sql');
SELECT RUN_METRIC('android/android_startup.sql');
```

```sql
INCLUDE PERFETTO MODULE sched.with_context;
INCLUDE PERFETTO MODULE linux.cpu.utilization.process;
INCLUDE PERFETTO MODULE linux.cpu.utilization.system;

SELECT c.runtime/1e9 AS runtime_s,
  c.runtime*1.0/(trace_end()-trace_start()) AS mean_busy_cores,
  c.runtime*1.0/(trace_end()-trace_start())/
    (SELECT count(*) FROM cpu_cycles_per_cpu) AS fraction_total_capacity
FROM cpu_cycles c;
SELECT p.name, c.upid, c.runtime/1e9 AS runtime_s, c.avg_freq
FROM cpu_cycles_per_process c JOIN process p USING (upid)
ORDER BY c.runtime DESC LIMIT 12;
SELECT s.utid, s.thread_name, sum(s.dur)/1e9 AS runtime_s,
  count(*) AS switches, max(s.dur)/1e6 AS max_sched_ms
FROM sched_with_thread_process s
WHERE s.upid=1017 AND s.dur>0
GROUP BY s.utid, s.thread_name ORDER BY runtime_s DESC LIMIT 25;
SELECT (c.ts-trace_start())/1e9 AS relative_s,
  c.utilization, c.unnormalized_utilization AS busy_cores
FROM cpu_process_utilization_per_second(1017) c;
SELECT (c.ts-trace_start())/1e9 AS relative_s,
  c.utilization, c.unnormalized_utilization AS busy_cores
FROM cpu_utilization_per_second c
ORDER BY c.utilization DESC LIMIT 10;
```

```sql
INCLUDE PERFETTO MODULE android.memory.process;
INCLUDE PERFETTO MODULE android.memory.lmk;
INCLUDE PERFETTO MODULE slices.with_context;

SELECT count(*) AS observations, min(m.rss)/1048576.0 AS min_rss_mib,
  max(m.rss)/1048576.0 AS max_rss_mib,
  min(m.swap)/1048576.0 AS min_swap_mib,
  max(m.swap)/1048576.0 AS max_swap_mib
FROM memory_oom_score_with_rss_and_swap_per_process m WHERE m.upid=1017;
SELECT cast((m.ts-trace_start())/5e9 AS INT)*5 AS time_bin_s,
  min(m.rss)/1048576.0 AS min_rss_mib,
  max(m.rss)/1048576.0 AS max_rss_mib
FROM memory_oom_score_with_rss_and_swap_per_process m
WHERE m.upid=1017 AND m.rss IS NOT NULL
GROUP BY cast((m.ts-trace_start())/5e9 AS INT)*5;
SELECT s.name, s.thread_name, count(*) AS calls,
  sum(s.dur)/1e6 AS total_ms, max(s.dur)/1e6 AS max_ms
FROM thread_or_process_slice s
WHERE s.upid=1017 AND s.dur>0 AND
  (s.name GLOB '*GC*' OR s.name GLOB '*gc*'
    OR s.name GLOB '*SuspendAll*' OR s.name GLOB '*Reclaim*')
GROUP BY s.name, s.thread_name ORDER BY total_ms DESC LIMIT 25;
SELECT s.name, s.process_name, s.upid, s.utid, s.thread_name,
  s.ts, s.dur, (s.ts-trace_start())/1e9 AS relative_s
FROM thread_or_process_slice s WHERE s.name='criticalLowMemory';
SELECT l.ts, l.upid, l.process_name, l.kill_reason
FROM android_lmk_events l;
```

```sql
INCLUDE PERFETTO MODULE linux.cpu.frequency;
SELECT c.cpu, count(*) AS samples,
  min(c.freq) AS min_khz, max(c.freq) AS max_khz
FROM cpu_frequency_counters c
WHERE c.ts>=trace_start()+30000000000
  AND c.ts<trace_start()+70000000000 GROUP BY c.cpu;

SELECT t.id, t.name, t.unit, count(*) AS samples,
  min(c.value) AS min_value, max(c.value) AS max_value,
  avg(c.value) AS sample_average
FROM counter c JOIN counter_track t ON t.id=c.track_id
WHERE c.ts>=trace_start()+30000000000
  AND c.ts<trace_start()+70000000000
  AND (t.name='gpufreq' OR t.name='Gpu Utilization Frequency'
    OR t.name='Gpu Max Limit Frequency'
    OR t.name='Gpu Min Limit Frequency' OR t.type='thermal_temperature')
GROUP BY t.id, t.name, t.unit;

SELECT t.id, t.name, count(*) AS samples,
  min(c.value)/1048576.0 AS min_gpu_mib,
  max(c.value)/1048576.0 AS max_gpu_mib
FROM counter c JOIN counter_track t ON t.id=c.track_id
WHERE t.type='process_gpu_memory'
  AND extract_arg(t.dimension_arg_set_id, 'upid')=1017
GROUP BY t.id, t.name;
```

## Map rendering and lifecycle queries

```sql
INCLUDE PERFETTO MODULE slices.with_context;
INCLUDE PERFETTO MODULE slices.cpu_time;
SELECT s.utid,s.name,count(*) AS frames,
       round(avg(s.dur)/1e6,3) AS avg_wall_ms,
       round(sum(c.cpu_time)/1e9,3) AS cpu_s,
       round(max(c.cpu_time)/1e6,3) AS max_cpu_ms
FROM thread_or_process_slice s
JOIN thread_slice_cpu_time c USING(id)
WHERE s.utid IN (10803,11704,11772) AND s.name='animation' AND s.dur>0
GROUP BY s.utid;
```

```sql
INCLUDE PERFETTO MODULE slices.with_context;
SELECT cast((s.ts-2982387674480863)/1e9 AS INT) AS second,
       count(*) AS renderframes,round(sum(s.dur)/1e6,1) AS wall_ms,
       round(max(s.dur)/1e6,3) AS max_ms
FROM thread_or_process_slice s
WHERE s.utid=10803 AND s.name='animation' AND s.dur>0
GROUP BY second ORDER BY second;
```

```sql
INCLUDE PERFETTO MODULE slices.with_context;
INCLUDE PERFETTO MODULE slices.cpu_time;
WITH frames AS (
 SELECT s.utid,s.dur,c.cpu_time,
  percent_rank() OVER(PARTITION BY s.utid ORDER BY s.dur) AS wall_p,
  percent_rank() OVER(PARTITION BY s.utid ORDER BY c.cpu_time) AS cpu_p
 FROM thread_or_process_slice s JOIN thread_slice_cpu_time c USING(id)
 WHERE s.utid IN(10803,11704,11772) AND s.name='animation' AND s.dur>0
)
SELECT f.utid,count(*) AS n,
 round(max(CASE WHEN f.wall_p<=0.5 THEN f.dur END)/1e6,3) AS wall_p50_ms,
 round(max(CASE WHEN f.wall_p<=0.95 THEN f.dur END)/1e6,3) AS wall_p95_ms,
 round(max(CASE WHEN f.wall_p<=0.99 THEN f.dur END)/1e6,3) AS wall_p99_ms,
 round(max(CASE WHEN f.cpu_p<=0.95 THEN f.cpu_time END)/1e6,3) AS cpu_p95_ms
FROM frames f GROUP BY f.utid;
```

```sql
INCLUDE PERFETTO MODULE intervals.intersect;
WITH states AS (
 SELECT st.id,st.ts,st.dur FROM thread_state st WHERE st.utid=10803 AND st.dur>0
), overlap AS (
 SELECT o.id,o.ts,o.dur FROM _interval_intersect_single!(2982417347724863,40550917000,states) o
)
SELECT st.state,round(sum(o.dur)/1e9,6) AS seconds,
       round(sum(o.dur)*100.0/40550917000,3) AS window_pct
FROM overlap o JOIN thread_state st USING(id)
GROUP BY st.state ORDER BY sum(o.dur) DESC;
```

```sql
INCLUDE PERFETTO MODULE slices.with_context;
WITH input_times AS (
 SELECT s.ts,s.name,lead(s.ts) OVER (ORDER BY s.ts) AS next_ts
 FROM thread_or_process_slice s
 WHERE s.utid=1106 AND s.name GLOB 'dispatchInputEvent MotionEvent *'
   AND s.ts BETWEEN 2982417674480863 AND 2982457674480863
)
SELECT round((i.ts-2982387674480863)/1e9,3) AS last_input_s,
       round((i.next_ts-2982387674480863)/1e9,3) AS next_input_s,
       round((i.next_ts-i.ts)/1e9,3) AS gap_s,i.name
FROM input_times i WHERE i.next_ts>i.ts
ORDER BY i.next_ts-i.ts DESC LIMIT 15;
```

```sql
INCLUDE PERFETTO MODULE slices.with_context;
INCLUDE PERFETTO MODULE slices.cpu_time;
SELECT count(*) AS frames,round(sum(s.dur)/1e9,6) AS wall_s,
       round(sum(c.cpu_time)/1e9,6) AS cpu_s,
       round(min(s.ts-2982387674480863)/1e9,6) AS first_s,
       round(max(s.ts+s.dur-2982387674480863)/1e9,6) AS last_s
FROM thread_or_process_slice s JOIN thread_slice_cpu_time c USING(id)
WHERE s.utid=10803 AND s.name='animation'
 AND s.ts>=2982442179480863 AND s.ts<2982454215480863;
```

```sql
INCLUDE PERFETTO MODULE slices.with_context;
SELECT s.utid,s.thread_name,s.name,count(*) AS count,
       round(sum(s.dur)/1e6,3) AS dur_ms,round(max(s.dur)/1e6,3) AS max_ms
FROM thread_or_process_slice s
WHERE s.utid IN(10803,11704,11772,9514,9515,11757) AND s.dur>0
GROUP BY s.utid,s.name ORDER BY sum(s.dur) DESC LIMIT 80;
```

```sql
INCLUDE PERFETTO MODULE slices.with_context;
SELECT s.name,count(*) AS n,round(sum(s.dur)/1e6,3) AS wall_ms,
       round(min(s.ts-2982387674480863)/1e9,6) AS first_s,
       round(max(s.ts+s.dur-2982387674480863)/1e9,6) AS last_s
FROM thread_or_process_slice s
WHERE s.utid=1106 AND s.name GLOB 'notifyAnimEnd-*'
 AND s.ts>=2982417674480863 AND s.ts<2982457674480863
GROUP BY s.name ORDER BY count(*) DESC LIMIT 15;
```

```sql
INCLUDE PERFETTO MODULE slices.with_context;
SELECT s.utid,
 round((min(s.ts)-2982387674480863)/1e9,6) AS first_frame_s,
 round((max(s.ts+s.dur)-2982387674480863)/1e9,6) AS last_frame_end_s,
 count(*) AS frames
FROM thread_or_process_slice s
WHERE s.utid IN(10803,11704,11772) AND s.name='animation' AND s.dur>0
GROUP BY s.utid;
```

## UI frame and blocking-chain queries

```sql
INCLUDE PERFETTO MODULE slices.with_context;
INCLUDE PERFETTO MODULE slices.cpu_time;
INCLUDE PERFETTO MODULE android.frames.timeline;
INCLUDE PERFETTO MODULE linux.cpu.frequency;
```

```sql
WITH frames AS (SELECT a.surface_frame_token,CASE WHEN a.layer_name GLOB '*AuthenticationActivity*' THEN 'Authentication' WHEN a.layer_name GLOB '*MainActivity*' THEN 'Main' ELSE 'Other' END AS activity,MIN(a.ts) AS ts,MAX(a.dur) AS dur,MAX(IIF(a.jank_type GLOB '*App Deadline Missed*',1,0)) AS app_deadline_missed,MAX(IIF(a.present_type GLOB '*Dropped*',1,0)) AS dropped,MAX(IIF(a.jank_type GLOB '*Buffer Stuffing*',1,0)) AS buffer_stuffing,MAX(IIF(a.jank_type GLOB '*SurfaceFlinger*',1,0)) AS sf_jank,COUNT(*) AS raw_rows FROM actual_frame_timeline_slice AS a WHERE a.upid=1017 AND a.dur>=0 AND a.surface_frame_token>0 GROUP BY a.surface_frame_token,activity) SELECT f.activity,COUNT(*) AS unique_tokens,SUM(f.raw_rows) AS raw_rows,SUM(f.app_deadline_missed) AS app_deadline_missed,ROUND(100.0*SUM(f.app_deadline_missed)/COUNT(*),3) AS missed_pct,SUM(f.dropped) AS dropped,SUM(IIF(f.dropped=1 AND f.app_deadline_missed=0,1,0)) AS dropped_without_app_miss,SUM(f.buffer_stuffing) AS buffer_stuffing,SUM(f.sf_jank) AS sf_jank,ROUND(MAX(f.dur)/1e6,3) AS max_ms FROM frames AS f GROUP BY f.activity ORDER BY f.activity;
```

```sql
WITH frames AS (SELECT a.surface_frame_token,CASE WHEN a.layer_name GLOB '*AuthenticationActivity*' THEN 'Authentication' ELSE 'Main' END AS activity,MIN(a.ts) AS ts,MAX(a.dur) AS dur,MAX(IIF(a.jank_type GLOB '*App Deadline Missed*',1,0)) AS missed,MAX(IIF(a.jank_severity_type='Full',1,0)) AS full FROM actual_frame_timeline_slice AS a WHERE a.upid=1017 AND a.dur>=0 AND a.surface_frame_token>0 GROUP BY a.surface_frame_token,activity) SELECT f.activity,COUNT(*) AS missed_frames,SUM(f.full) AS full_severity,SUM(IIF(f.dur>16666667,1,0)) AS over_16_67ms,SUM(IIF(f.dur>25000000,1,0)) AS over_25ms,SUM(IIF(f.dur>33000000,1,0)) AS over_33ms,ROUND(MAX(f.dur)/1e6,3) AS max_ms FROM frames AS f WHERE f.missed=1 GROUP BY f.activity;
```

```sql
CREATE OR REPLACE PERFETTO TABLE fa_candidates AS SELECT s.id AS candidate_id,s.ts,s.dur,s.utid,s.name FROM thread_slice AS s WHERE s.id IN (472722,2959785,2958517,2911986,2912263); CREATE OR REPLACE PERFETTO TABLE fa_states AS SELECT t.id AS state_id,t.ts,t.dur,t.utid,t.state,t.cpu,t.waker_utid,t.waker_id,t.blocked_function,t.io_wait FROM thread_state AS t WHERE t.utid IN (1106,9514) AND t.dur>0; DROP TABLE IF EXISTS fa_statejoin; CREATE VIRTUAL TABLE fa_statejoin USING SPAN_JOIN(fa_candidates PARTITIONED utid, fa_states PARTITIONED utid); SELECT j.candidate_id,j.utid,j.state,ROUND(SUM(j.dur)/1e6,3) AS ms,COUNT(*) AS segments FROM fa_statejoin AS j GROUP BY j.candidate_id,j.utid,j.state ORDER BY j.candidate_id,ms DESC;
```

```sql
SELECT j.candidate_id,j.state,ROUND(SUM(j.dur)/1e6,3) AS ms,COUNT(*) AS segments FROM fa_statejoin AS j GROUP BY j.candidate_id,j.state ORDER BY j.candidate_id,ms DESC;
```

```sql
SELECT j.candidate_id,j.state_id,j.ts,ROUND((j.ts-2982387674480863)/1e9,6) AS offset_s,ROUND(j.dur/1e6,3) AS ms,j.state,j.blocked_function,j.waker_id,j.waker_utid,w.name AS waker_thread,p.name AS waker_process FROM fa_statejoin AS j LEFT JOIN thread AS w ON j.waker_utid=w.utid LEFT JOIN process AS p ON w.upid=p.upid WHERE j.state!='Running' AND j.dur>1000000 ORDER BY j.candidate_id,j.dur DESC;
```

```sql
SELECT s.id,ROUND((s.ts-2982387674480863)/1e9,6) AS offset_s,ROUND(s.dur/1e6,3) AS wall_ms,ROUND(c.cpu_time/1e6,3) AS cpu_ms,s.name,s.depth FROM thread_slice AS s LEFT JOIN thread_slice_cpu_time AS c ON s.id=c.id WHERE s.id IN (472722,472729,472747,472782,473883,474143,476995,477071,477081,477516,2959785,2960427,2962628) ORDER BY s.ts;
```

```sql
SELECT s.id,s.ts,ROUND((s.ts-2982387674480863)/1e9,6) AS offset_s,ROUND(s.dur/1e6,3) AS wall_ms,ROUND(c.cpu_time/1e6,3) AS cpu_ms,s.utid,s.thread_name,s.depth,s.name,s.parent_id FROM thread_slice AS s LEFT JOIN thread_slice_cpu_time AS c ON s.id=c.id WHERE s.upid=1017 AND s.utid IN (1106,9514,9515,11757) AND s.ts BETWEEN 2982479730679887 AND 2982479973445212 AND s.dur>1000000 ORDER BY s.ts,s.depth LIMIT 100;
```

```sql
SELECT s.id,s.utid,s.thread_name,ROUND((s.ts-2982387674480863)/1e9,6) AS offset_s,ROUND(s.dur/1e6,3) AS ms,ROUND(c.cpu_time/1e6,3) AS cpu_ms,s.name FROM thread_slice AS s LEFT JOIN thread_slice_cpu_time AS c ON s.id=c.id WHERE s.upid=1017 AND s.name GLOB '*CompileAfterCacheMiss*' ORDER BY s.dur DESC LIMIT 25;
```

```sql
SELECT s.id,ROUND((s.ts-2982387674480863)/1e9,6) AS offset_s,ROUND(s.dur/1e6,3) AS wall_ms,ROUND(c.cpu_time/1e6,3) AS cpu_ms,s.utid,s.thread_name,s.name FROM thread_slice AS s LEFT JOIN thread_slice_cpu_time AS c ON s.id=c.id WHERE s.id IN (2911559,2911986,2912544,2912546,2912263,2912363,2913177) ORDER BY s.ts;
```

```sql
SELECT j.state,j.state_id,ROUND((j.ts-2982387674480863)/1e9,6) AS offset_s,ROUND(j.dur/1e6,3) AS ms,j.cpu,j.io_wait,j.blocked_function,j.waker_utid FROM fa_statejoin AS j WHERE j.candidate_id=2911986 AND j.state!='Running' ORDER BY j.ts;
```

```sql
CREATE OR REPLACE PERFETTO TABLE fa_running AS SELECT j.ts,j.dur,j.cpu,j.candidate_id,j.utid FROM fa_statejoin AS j WHERE j.state='Running' AND j.cpu IS NOT NULL; CREATE OR REPLACE PERFETTO TABLE fa_frequencies AS SELECT f.ts,f.dur,f.cpu,f.freq FROM cpu_frequency_counters AS f WHERE f.dur>0; DROP TABLE IF EXISTS fa_running_freq; CREATE VIRTUAL TABLE fa_running_freq USING SPAN_JOIN(fa_running PARTITIONED cpu, fa_frequencies PARTITIONED cpu); SELECT r.candidate_id,r.utid,r.cpu,ROUND(SUM(r.dur)/1e6,3) AS cpu_ms,MIN(r.freq)/1000 AS min_mhz,MAX(r.freq)/1000 AS max_mhz FROM fa_running_freq AS r GROUP BY r.candidate_id,r.utid,r.cpu ORDER BY r.candidate_id,cpu_ms DESC;
```

## Additional log and capture coverage queries

```sql
SELECT COUNT(*) AS app_cpu_samples
FROM cpu_profile_stack_sample s JOIN thread t USING(utid)
WHERE t.upid=1017;

SELECT t.utid,t.name,(t.start_ts-trace_start())/1e9 AS start_s,
  (t.end_ts-trace_start())/1e9 AS end_s
FROM thread t WHERE t.upid=1017 AND t.name GLOB 'MbxRender*';

SELECT SUM(CASE WHEN l.tag='AndroidRuntime' AND l.msg GLOB '*FATAL*' AND t.upid=1017 THEN 1 ELSE 0 END) AS app_fatal_logs, SUM(CASE WHEN (l.tag='am_anr' OR lower(l.msg) GLOB '*anr in*' OR lower(l.msg) GLOB '*anr*com.kumpello.whereiseveryone*') AND l.msg GLOB '*com.kumpello.whereiseveryone*' THEN 1 ELSE 0 END) AS app_anr_logs FROM android_logs l LEFT JOIN thread t USING(utid);

SELECT ROUND((l.ts-2982387674480863)/1e9,3) AS start_s,l.utid,l.prio,l.tag,l.msg FROM android_logs l JOIN thread t USING(utid) WHERE t.upid=1017 AND l.prio>=5 ORDER BY l.ts LIMIT 100;
```

## Vulkan initialization timings

```sql
INCLUDE PERFETTO MODULE slices.with_context;
SELECT s.utid,s.thread_name,s.ts,ROUND((s.ts-trace_start())/1e9,6) AS offset_s,ROUND(s.dur/1e6,3) AS wall_ms,s.name
FROM thread_or_process_slice s
WHERE s.utid IN(10803,11704,11772)
  AND (s.name GLOB '*CreateInstance*' OR s.name GLOB '*CreateDevice*' OR s.name GLOB '*CreateSwapchain*')
ORDER BY s.ts;
```
