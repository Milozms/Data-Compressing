f1 = open('1.jar', 'rb')
f2 = open('1decode.jar', 'rb')
s1 = f1.read()
s2 = f2.read()
l = len(s1)
count = 0
for i in range(0, l):
    if s1[i] != s2[i]:
        print(i)
        count += 1
print(l, count)
