def reverse_pad_list(ys_pad: Tensor,
    ys_lens: Tensor,
    pad_value: float=-1.) -> Tensor:
  _0 = __torch__.torch.nn.utils.rnn.pad_sequence
  _1 = annotate(List[Tensor], [])
  _2 = [torch.len(ys_pad), torch.len(ys_lens)]
  for _3 in range(ops.prim.min(_2)):
    y = torch.select(ys_pad, 0, _3)
    i = torch.select(ys_lens, 0, _3)
    _4 = torch.slice(torch.to(y, 3, False, False, None), 0, 0, annotate(int, i), 1)
    _5 = torch.append(_1, torch.flip(_4, [0]))
  return _0(_1, True, pad_value, )
def add_sos_eos(ys_pad: Tensor,
    sos: int,
    eos: int,
    ignore_id: int) -> Tuple[Tensor, Tensor]:
  _sos = torch.tensor([sos], dtype=4, device=ops.prim.device(ys_pad), requires_grad=False)
  _eos = torch.tensor([eos], dtype=4, device=ops.prim.device(ys_pad), requires_grad=False)
  ys = annotate(List[Tensor], [])
  for _6 in range(torch.len(ys_pad)):
    y = torch.select(ys_pad, 0, _6)
    _7 = annotate(List[Optional[Tensor]], [torch.ne(y, ignore_id)])
    _8 = torch.append(ys, torch.index(y, _7))
  ys_in = annotate(List[Tensor], [])
  for _9 in range(torch.len(ys)):
    y0 = ys[_9]
    _10 = torch.append(ys_in, torch.cat([_sos, y0], 0))
  ys_out = annotate(List[Tensor], [])
  for _11 in range(torch.len(ys)):
    y1 = ys[_11]
    _12 = torch.append(ys_out, torch.cat([y1, _eos], 0))
  _13 = __torch__.wenet.utils.common.pad_list(ys_in, eos, )
  _14 = __torch__.wenet.utils.common.pad_list(ys_out, ignore_id, )
  return (_13, _14)
def th_accuracy(pad_outputs: Tensor,
    pad_targets: Tensor,
    ignore_label: int) -> float:
  _15 = [torch.size(pad_targets, 0), torch.size(pad_targets, 1), torch.size(pad_outputs, 1)]
  pad_pred = torch.argmax(torch.view(pad_outputs, _15), 2, False)
  mask = torch.ne(pad_targets, ignore_label)
  _16 = torch.masked_select(pad_pred, mask)
  _17 = torch.masked_select(pad_targets, mask)
  numerator = torch.sum(torch.eq(_16, _17), dtype=None)
  denominator = torch.sum(mask, dtype=None)
  _18 = torch.div(float(numerator), float(denominator))
  return _18
def pad_list(xs: List[Tensor],
    pad_value: int) -> Tensor:
  n_batch = torch.len(xs)
  _19 = annotate(List[int], [])
  for _20 in range(torch.len(xs)):
    x = xs[_20]
    _21 = torch.append(_19, torch.size(x, 0))
  max_len = ops.prim.max(_19)
  _22 = ops.prim.dtype(xs[0])
  _23 = ops.prim.device(xs[0])
  pad = torch.zeros([n_batch, max_len], dtype=_22, layout=None, device=_23, pin_memory=None)
  pad0 = torch.fill_(pad, pad_value)
  for i in range(n_batch):
    _24 = xs[i]
    _25 = torch.slice(torch.select(pad0, 0, i), 0, 0, torch.size(xs[i], 0), 1)
    _26 = torch.copy_(_25, _24, False)
  return pad0
