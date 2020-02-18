# Backend as CG function

## deployment

    pick-n-search/backend$ gcloud functions deploy delete --runtime python37 --trigger-http --allow-unauthenticated

## read logs

    gcloud functions logs read publish

## test

### publish

    curl -d '{"key1":"value1", "key2":"value2"}' -H "Content-Type: application/json" -X POST https://us-central1-pick-n-search.cloudfunctions.net/publish


### read

    curl https://us-central1-pick-n-search.cloudfunctions.net/read

### delete

    https://us-central1-pick-n-search.cloudfunctions.net/delete?id=(62703,28,-1,-1)

