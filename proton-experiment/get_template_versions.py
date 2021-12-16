import sys, json
data = json.load(sys.stdin)
for templateVersion in data['templateVersions']:
    print(f'{templateVersion["majorVersion"]} {templateVersion["minorVersion"]}')
