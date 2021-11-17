class Sequential(Module):
  __parameters__ = []
  __buffers__ = []
  training : bool
  __annotations__["0"] = __torch__.torch.nn.quantized.dynamic.modules.linear.Linear
  def forward(self: __torch__.torch.nn.modules.container.___torch_mangle_15.Sequential,
    input: Tensor) -> Tensor:
    input0 = (getattr(self, "0")).forward(input, )
    return input0
