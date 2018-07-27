This is a super dumb http server to accept posted data, and
redisplay it.

## Setup instructions

I recommend using virtualenv. I hate it too. System packages for flask
are horribly outdated on so many systems, and it goes subtly wrong if you're
not using a recent version.

```
python3 -m venv .venv
. .venv/bin/activate
pip3 install -r requirements.txt
flask run
```

On some machines, this still tries to run the wrong `flask`, and you will
get `python2.7` (??? what year is it) errors about invalid syntax.
In this case, `.venv/bin/flask run` can be used, instead of the last line.


## Default url:

[http://localhost:5000/](http://localhost:5000/)


## jquery and python??

Yeah, I feel you. No, I didn't think it was a good idea when I started.
No, it wasn't a good idea.

I wanted something working on someone else's mac in under a day.
What would you have done?
