import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class VpnScreen extends StatefulWidget {
  const VpnScreen({super.key});

  @override
  State<VpnScreen> createState() => _VpnScreenState();
}

class _VpnScreenState extends State<VpnScreen> {
  static const platform = MethodChannel('site_blocker');
  bool _isVpnOn = false;
  final TextEditingController _controller = TextEditingController();
  final List<String> _blockedSites = [
    'youtube.com',
    'facebook.com',
    'instagram.com',
  ];

  @override
  void initState() {
    super.initState();
    _checkVpnStatus();
  }

  Future<void> _checkVpnStatus() async {
    try {
      final bool status = await platform.invokeMethod('isVpnRunning');
      setState(() => _isVpnOn = status);
    } catch (e) {
      setState(() => _isVpnOn = false);
    }
  }

  Future<void> _toggleVpn() async {
    try {
      if (_isVpnOn) {
        await platform.invokeMethod('stopVpn');
        setState(() => _isVpnOn = false);
      } else {
        await platform.invokeMethod('startVpn');
        setState(() => _isVpnOn = true);
      }
    } catch (e) {
      debugPrint("VPN error: $e");
    }
  }

  Future<void> _blockSite(String site) async {
    try {
      await platform.invokeMethod('blockSite', {'url': site});
      setState(() => _blockedSites.add(site));
    } catch (e) {
      debugPrint("Block error: $e");
    }
  }

  Future<void> _unblockSite(String site) async {
    try {
      await platform.invokeMethod('unblockSite', {'url': site});
      setState(() => _blockedSites.remove(site));
    } catch (e) {
      debugPrint("Unblock error: $e");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Site Blocker"),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black,
        elevation: 0.5,
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            // VPN Toggle
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: _isVpnOn
                    ? Colors.green.withOpacity(0.1)
                    : Colors.red.withOpacity(0.1),
                borderRadius: BorderRadius.circular(16),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        _isVpnOn ? "🟢 VPN ON" : "🔴 VPN OFF",
                        style: const TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        _isVpnOn
                            ? "Blocking active"
                            : "Tap to enable blocking",
                        style: const TextStyle(
                          fontSize: 13,
                          color: Colors.black45,
                        ),
                      ),
                    ],
                  ),
                  Switch(
                    value: _isVpnOn,
                    onChanged: (_) => _toggleVpn(),
                    activeColor: Colors.green,
                  ),
                ],
              ),
            ),

            const SizedBox(height: 20),

            // Add site to block
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _controller,
                    decoration: InputDecoration(
                      hintText: "e.g. youtube.com",
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 14,
                        vertical: 10,
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                ElevatedButton(
                  onPressed: () {
                    if (_controller.text.isNotEmpty) {
                      _blockSite(_controller.text.trim());
                      _controller.clear();
                    }
                  },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.blue,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                    padding: const EdgeInsets.symmetric(
                      horizontal: 20,
                      vertical: 12,
                    ),
                  ),
                  child: const Text(
                    "Block",
                    style: TextStyle(color: Colors.white),
                  ),
                ),
              ],
            ),

            const SizedBox(height: 16),

            const Align(
              alignment: Alignment.centerLeft,
              child: Text(
                "Blocked Sites",
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF1A1D2E),
                ),
              ),
            ),

            const SizedBox(height: 8),

            // Blocked sites list
            Expanded(
              child: _blockedSites.isEmpty
                  ? const Center(
                      child: Text(
                        "No sites blocked yet",
                        style: TextStyle(color: Colors.black45),
                      ),
                    )
                  : ListView.builder(
                      itemCount: _blockedSites.length,
                      itemBuilder: (context, index) {
                        return Card(
                          margin: const EdgeInsets.only(bottom: 8),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: ListTile(
                            leading: const Icon(
                              Icons.block,
                              color: Colors.red,
                            ),
                            title: Text(_blockedSites[index]),
                            trailing: IconButton(
                              icon: const Icon(
                                Icons.delete,
                                color: Colors.red,
                              ),
                              onPressed: () =>
                                  _unblockSite(_blockedSites[index]),
                            ),
                          ),
                        );
                      },
                    ),
            ),
          ],
        ),
      ),
    );
  }
}