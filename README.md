# blog-comments-public

Backend to collect comments made on my blog

`.env` file is needed for `docker-compose up`:

| Variable       | default                  | Purpose                        |
| -------------- | ------------------------ | ------------------------------ |
| ClientSecret   | ./.                      | OAuth for Repo                 |
| ClientToken    | ./.                      | OAuth for Repo                 |
| PushToken      | ./.                      | OAuth for Push Notification    |
| PushUser       | ./.                      | OAuth for Push Notification    |
| OauthURL       | bitbucket.org            | IdP for auth                   |
| RepositoryURL  | stwissel/blogsource      | Which repo to get the PR       |
| PORT           | 8080                     | Where does the app listen      |
| CaptchaSecret  | ./.                      | to Validate captcha            |
| ACTIVITY_USERS | /opt/activity/users.json | Users for mastodon .well-known |

## users.json for Mastodon

```json
{
  "some@e.mail": {
    "userid": "some",
    "domain": "mastodon.social"
  }
}
```
