"""Accumulate GitHub traffic for this repository into CSV files.

GitHub keeps traffic (views, clones, referrers, popular paths) for 14 days only,
so a spike can no longer be explained once two weeks pass. This script is run
daily by .github/workflows/traffic-stats.yml and merges the API's 14-day window
into files on the `traffic-stats` branch, so the history outlives that window.

Usage: python scripts/collect_traffic.py <data-dir>
Environment: TRAFFIC_TOKEN (push access to the repository; GITHUB_TOKEN cannot
read traffic), GITHUB_REPOSITORY (owner/name).

Files written to <data-dir>:
  views.csv, clones.csv   date,count,uniques       one row per day, merged by date
  referrers.csv, paths.csv snapshot rows            the API only gives 14-day totals,
                                                    so each run appends that day's list
  repo.csv                date,stars,forks,watchers,open_issues,open_prs
"""

import csv
import datetime as dt
import json
import os
import sys
import urllib.request

API = "https://api.github.com"


def get(path, token):
    request = urllib.request.Request(
        f"{API}{path}",
        headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {token}",
            "X-GitHub-Api-Version": "2022-11-28",
        },
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def read_rows(path):
    if not os.path.exists(path):
        return []
    with open(path, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def write_rows(path, fields, rows):
    with open(path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fields, lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)


def merge_daily(path, entries):
    """Merge per-day counts by date. The API's newest values win: today's row is
    partial when collected and is completed by the next run."""
    by_date = {row["date"]: row for row in read_rows(path)}
    for entry in entries:
        date = entry["timestamp"][:10]
        by_date[date] = {"date": date, "count": entry["count"], "uniques": entry["uniques"]}
    write_rows(path, ["date", "count", "uniques"], [by_date[d] for d in sorted(by_date)])


def append_snapshot(path, fields, today, rows):
    """Replace today's snapshot (a rerun the same day must not duplicate it)."""
    kept = [row for row in read_rows(path) if row["snapshot_date"] != today]
    write_rows(path, fields, kept + [{"snapshot_date": today, **row} for row in rows])


def main():
    if len(sys.argv) != 2:
        sys.exit("usage: collect_traffic.py <data-dir>")
    data_dir = sys.argv[1]
    token = os.environ.get("TRAFFIC_TOKEN")
    repo = os.environ.get("GITHUB_REPOSITORY")
    if not token:
        sys.exit("TRAFFIC_TOKEN is not set; GitHub traffic cannot be read without push access.")
    if not repo:
        sys.exit("GITHUB_REPOSITORY is not set.")

    today = dt.datetime.now(dt.timezone.utc).date().isoformat()
    base = f"/repos/{repo}"

    merge_daily(os.path.join(data_dir, "views.csv"), get(f"{base}/traffic/views?per=day", token)["views"])
    merge_daily(os.path.join(data_dir, "clones.csv"), get(f"{base}/traffic/clones?per=day", token)["clones"])

    append_snapshot(
        os.path.join(data_dir, "referrers.csv"),
        ["snapshot_date", "referrer", "count", "uniques"],
        today,
        [
            {"referrer": r["referrer"], "count": r["count"], "uniques": r["uniques"]}
            for r in get(f"{base}/traffic/popular/referrers", token)
        ],
    )
    append_snapshot(
        os.path.join(data_dir, "paths.csv"),
        ["snapshot_date", "path", "count", "uniques"],
        today,
        [
            {"path": p["path"], "count": p["count"], "uniques": p["uniques"]}
            for p in get(f"{base}/traffic/popular/paths", token)
        ],
    )

    info = get(base, token)
    # The repository's open_issues_count also counts open pull requests, so the
    # two are counted separately through search.
    open_issues = get(f"/search/issues?q=repo:{repo}+is:issue+is:open&per_page=1", token)["total_count"]
    open_prs = get(f"/search/issues?q=repo:{repo}+is:pr+is:open&per_page=1", token)["total_count"]
    repo_path = os.path.join(data_dir, "repo.csv")
    rows = [row for row in read_rows(repo_path) if row["date"] != today]
    rows.append(
        {
            "date": today,
            "stars": info["stargazers_count"],
            "forks": info["forks_count"],
            "watchers": info["subscribers_count"],
            "open_issues": open_issues,
            "open_prs": open_prs,
        }
    )
    write_rows(repo_path, ["date", "stars", "forks", "watchers", "open_issues", "open_prs"], rows)


if __name__ == "__main__":
    main()
