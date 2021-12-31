translation to non-destructive form

a.f = v
---
a_1 = a_0.updated(f = v)

a.f.g = v
---
a_1 = a_0.updated(f = a_0.f.updated(g = v))

a[i] = v
---
a_1 = a_0.updated_at(i, v)

a.f[i] = v
---
a_1 = a_0.updated(f = a.f.updated_at(i, v))

r = f(a,b,c)
---
r, a_1, b_1, c_1 = f_(a, b, c)


f(e1,e2,e3)
t1 = e1
t2 = e2
t3 = e3
f(t1,t2,t3)

slot < state.slot <= slot + C

let tmp_o = state.slots[0]
in tmp_o <= slot + C if (slot < tmp_o) else false

(lamba tmp_0: tmp_o <= slot + C if (slot < tmp_o) else false)(state.slot)