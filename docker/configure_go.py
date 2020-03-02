import json
import os

data = json.loads(os.environ['v_src'])

for path, contents in data.items():
    # Make any parent directories
    os.makedirs(os.path.dirname(path), exist_ok=True)

    with open(path, 'w') as file:
        file.write(contents)

