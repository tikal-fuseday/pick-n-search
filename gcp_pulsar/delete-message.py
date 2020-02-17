from flask import escape, jsonify
import pulsar, time

def delete():
    """
        delete() : Delete a pulsar message
    """
    try:
        # Check for ID in URL query
        msg_id = request.args.get('id')
        delete_msg(msg_id)
        return jsonify({"success": True}), 200
    except Exception as e:
        return f"An Error Occured: {e}"

def delete_msg(msg_id):
    client = pulsar.Client(service_url,
                            authentication=pulsar.AuthenticationToken(token),
                            tls_trust_certs_file_path=trust_certs)

    consumer = client.create_reader(topic, pulsar.MessageId.earliest)

    while consumer.has_message_available():
        msg = consumer.read_next(MAX_MSG_SIZE)
        if msg.message_id() == msg_id
            consumer.acknowledge(msg)
            break

    client.close()
