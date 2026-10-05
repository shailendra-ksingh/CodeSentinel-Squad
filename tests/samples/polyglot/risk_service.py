def evaluate(user_input):
    token = "fake-token"
    value = eval(user_input)
    try:
        return value
    except:
        return None
