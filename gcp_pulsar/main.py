from flask import escape, jsonify
import pulsar, time

service_url = 'pulsar+ssl://useast1.gcp.kafkaesque.io:6651'

# Use default CA certs for your environment
# RHEL/CentOS:
# trust_certs='/etc/ssl/certs/ca-bundle.crt'
# Debian/Ubuntu:
trust_certs='./ca-certificates.crt'
# OSX:
# Export the default certificates to a file, then use that file:
#    security find-certificate -a -p /System/Library/Keychains/SystemCACertificates.keychain > ./ca-certificates.crt
# trust_certs='./ca-certificates.crt'

token='eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJmdXplMjBnOC10aWthbGstY29tLWNsaWVudC01ZTRhNmY4ZThiOTBjIn0.G8JrP2Hl9zqVW1Ihx_6ekkPzOHcKjb-XwsOjUMaueLyZhnRbhNEONsO4nzuU00lROHSC3aU1C_2qiIQUsPNmpxUCze5aQBzcdWaMkYa7R5bowZ_onstof6n-2G1Hs8CudwWhfEvQzngUef55kdAYa6AQQgEP3jQB1mNlxhBQ6Tz6yN0tzIXCSwr2TbMxvGMuGS4Iv4eqICqZcrg0UWa1EPkwW1lblK4_bxAB96Yu4CuO0-DJVg09ovvsUcH6tfXonkWPVMkgbtN3H0zwLzTpubCjE6nY7bOkvKM7nRk9OvOXmG-7FPT4lizgp0d1S9VRfr7woB6YqHyb_SwEmeS1Nw'

topic = 'fuze20g8-tikalk-com/local-useast1-gcp/test-topic'

MAX_MSG_SIZE = 5000

def read_all():
    client = pulsar.Client(service_url,
                            authentication=pulsar.AuthenticationToken(token),
                            tls_trust_certs_file_path=trust_certs)

    consumer = client.create_reader(topic, pulsar.MessageId.earliest)

    messages = []
    while consumer.has_message_available():
        msg = consumer.read_next(MAX_MSG_SIZE)
        print("Received message '{}' id='{}'".format(msg.data(), msg.message_id()))
        message = {}
        message["id"] = str(msg.message_id())
        message["data"] = str(msg.data())
        messages.append(message)
    client.close()

    return messages


def read(request):
    """HTTP Cloud Function.
    Args:
        request (flask.Request): The request object.
        <http://flask.pocoo.org/docs/1.0/api/#flask.Request>
    Returns:
        The response text, or any set of values that can be turned into a
        Response object using `make_response`
        <http://flask.pocoo.org/docs/1.0/api/#flask.Flask.make_response>.
    """
    request_json = request.get_json(silent=True)
    request_args = request.args

    if request_json and 'name' in request_json:
        name = request_json['name']
    elif request_args and 'name' in request_args:
        name = request_args['name']
    else:
        name = 'World'

    messages = read_all()
    response = jsonify(messages)
    response.headers.set('Access-Control-Allow-Origin', '*')
    response.headers.set('Access-Control-Allow-Methods', 'GET, POST')
    return response
