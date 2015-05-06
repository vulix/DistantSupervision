from tweepy.streaming import StreamListener
from tweepy import OAuthHandler
from tweepy import Stream
from pymongo import MongoClient
from time import gmtime, strftime
import json
import sys

# Go to http://dev.twitter.com and create an app.
# The consumer key and secret will be generated for you after
consumer_key="1Axtc66RkXLkjINmUxeCg"
consumer_secret="IWd31qkvJVRUbyKrecJHAAsb70mowAYi6DXGcwGbIw"

# After the step above, you will be redirected to your app's page.
# Create an access token under the the "Your access token" section
access_token="162633131-OQ3JnC1tNf52CGX2rw3OoixlkjOhwYq6lVtUf4V9"
access_token_secret="ub8qIJlMNo5vvZ1uSnavcntAgzw306ORvce04j9la7sru"

class StdOutListener(StreamListener):
    """ A listener handles tweets are the received from the stream.
    This is a basic listener that just prints received tweets to stdout.

    """
    def on_data(self, data):
        client=MongoClient('mongodb://127.0.0.1:27017/')
        db=client.diabetes
        collection =db.treatments
        tweet = json.loads(data)
        post_id = collection.insert(tweet)
        print("Inserting: %s \n" %(post_id))
        print tweet['lang']
        return True

    def on_error(self, status):
        print status

if __name__ == '__main__':
    l = StdOutListener()
    auth = OAuthHandler(consumer_key, consumer_secret)
    auth.set_access_token(access_token, access_token_secret)

    stream = Stream(auth, l)
    stream.filter(track=['glipizide', 'glucotrol', 'Glyburide', 'Micronase', 'Glynase','Avandia','Diabeta', 'avandia','Glimepiride', 'Januvia', 'Amaryl', 'Metformin','Repaglinide', 'glucophage','Prandin','Nateglinide', 'Starlix','Rosiglitazone', 'pioglitazone','Actos', 'Sitagliptin','Sitagliptin','Januvia','Saxagliptin', 'Onglyza','linagliptin','Tradjenta','alogliptin','Nesina', 'Canagliflozin','Invokana','Dapagliflozin', 'Farxiga','Welchol', 'Insulin','apidra', 'humalog','novolog','humulin','novolin','levemir','afrezza','lantus'])
